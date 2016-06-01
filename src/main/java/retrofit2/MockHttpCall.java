/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response.Builder;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import retrofit2.mock.NetworkBehavior;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

final class MockHttpCall<T> implements Call<T> {
    private final ServiceMethod<T> serviceMethod;
    private final Object[] args;
    private NetworkBehavior behavior;
    private String rawResponse;
    private ExecutorService executorService;
    private volatile Future<?> task;
    private volatile boolean canceled;

    // All guarded by this.
    private boolean executed;

    MockHttpCall(ServiceMethod<T> serviceMethod, Object[] args, NetworkBehavior behavior,ExecutorService executorService, String rawResponse) {
        this.serviceMethod = serviceMethod;
        this.args = args;
        this.behavior = behavior;
        this.rawResponse = rawResponse;
        this.executorService = executorService;
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    // We are a final type & this saves clearing state.
    @Override
    public MockHttpCall<T> clone() {
        return new MockHttpCall<>(serviceMethod, args, behavior, executorService, rawResponse);
    }

    @Override
    public synchronized Request request() {
        try {
            return serviceMethod.toRequest(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void enqueue(final Callback<T> callback) {
        if (callback == null) throw new NullPointerException("callback == null");

        synchronized (this) {
            if (executed) throw new IllegalStateException("Already executed");
            executed = true;
        }
        task = executorService.submit(new Runnable() {
            boolean delaySleep() {
                long sleepMs = behavior.calculateDelay(MILLISECONDS);
                if (sleepMs > 0) {
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException e) {
                        callback.onFailure(MockHttpCall.this, new IOException("canceled"));
                        return false;
                    }
                }
                return true;
            }

            @Override public void run() {
                if (canceled) {
                    callback.onFailure(MockHttpCall.this, new IOException("canceled"));
                } else if (behavior.calculateIsFailure()) {
                    if (delaySleep()) {
                        callback.onFailure(MockHttpCall.this, behavior.failureException());
                    }
                } else {
                    try {
                        Response response = execute();
                        if (delaySleep()) {
                            callback.onResponse(MockHttpCall.this, response);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (delaySleep()) {
                            callback.onFailure(MockHttpCall.this, e);
                        }
                    };
                }
            }
        });
    }

    @Override
    public synchronized boolean isExecuted() {
        return executed;
    }

    @Override
    public Response<T> execute() throws IOException {
        Builder builder = new Builder();
        builder.body(ResponseBody.create(MediaType.parse("application/json"), rawResponse.getBytes()))
                .code(200)
                .request(request())
                .protocol(Protocol.HTTP_1_1);
        return parseResponse(builder.build());
    }

    Response<T> parseResponse(okhttp3.Response rawResponse) throws IOException {
        ResponseBody rawBody = rawResponse.body();

        // Remove the body's source (the only stateful object) so we can pass the response along.
        rawResponse = rawResponse.newBuilder()
                .body(new NoContentResponseBody(rawBody.contentType(), rawBody.contentLength()))
                .build();

        int code = rawResponse.code();
        if (code < 200 || code >= 300) {
            try {
                // Buffer the entire body to avoid future I/O.
                ResponseBody bufferedBody = Utils.buffer(rawBody);
                return Response.error(bufferedBody, rawResponse);
            } finally {
                rawBody.close();
            }
        }

        if (code == 204 || code == 205) {
            return Response.success(null, rawResponse);
        }

        ExceptionCatchingRequestBody catchingBody = new ExceptionCatchingRequestBody(rawBody);
        try {
            T body = serviceMethod.toResponse(catchingBody);
            return Response.success(body, rawResponse);
        } catch (RuntimeException e) {
            // If the underlying source threw an exception, propagate that rather than indicating it was
            // a runtime exception.
            catchingBody.throwIfCaught();
            throw e;
        }
    }

    public void cancel() {
        canceled = true;
        Future<?> task = this.task;
        if (task != null) {
            task.cancel(true);
        }
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    static final class NoContentResponseBody extends ResponseBody {
        private final MediaType contentType;
        private final long contentLength;

        NoContentResponseBody(MediaType contentType, long contentLength) {
            this.contentType = contentType;
            this.contentLength = contentLength;
        }

        @Override
        public MediaType contentType() {
            return contentType;
        }

        @Override
        public long contentLength() {
            return contentLength;
        }

        @Override
        public BufferedSource source() {
            throw new IllegalStateException("Cannot read raw response body of a converted body.");
        }
    }

    static final class ExceptionCatchingRequestBody extends ResponseBody {
        private final ResponseBody delegate;
        IOException thrownException;

        ExceptionCatchingRequestBody(ResponseBody delegate) {
            this.delegate = delegate;
        }

        @Override
        public MediaType contentType() {
            return delegate.contentType();
        }

        @Override
        public long contentLength() {
            return delegate.contentLength();
        }

        @Override
        public BufferedSource source() {
            return Okio.buffer(new ForwardingSource(delegate.source()) {
                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    try {
                        return super.read(sink, byteCount);
                    } catch (IOException e) {
                        thrownException = e;
                        throw e;
                    }
                }
            });
        }

        @Override
        public void close() {
            delegate.close();
        }

        void throwIfCaught() throws IOException {
            if (thrownException != null) {
                throw thrownException;
            }
        }
    }
}
