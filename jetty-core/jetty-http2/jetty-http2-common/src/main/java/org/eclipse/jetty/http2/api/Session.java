//
// ========================================================================
// Copyright (c) 1995-2022 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.http2.api;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jetty.http2.frames.GoAwayFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.frames.PingFrame;
import org.eclipse.jetty.http2.frames.PriorityFrame;
import org.eclipse.jetty.http2.frames.ResetFrame;
import org.eclipse.jetty.http2.frames.SettingsFrame;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Promise;

/**
 * <p>A {@link Session} represents the client-side endpoint of an HTTP/2 connection to a single origin server.</p>
 * <p>Once a {@link Session} has been obtained, it can be used to open HTTP/2 streams:</p>
 * <pre>
 * Session session = ...;
 * HeadersFrame frame = ...;
 * Promise&lt;Stream&gt; promise = ...
 * session.newStream(frame, promise, new Stream.Listener()
 * {
 *     public void onHeaders(Stream stream, HeadersFrame frame)
 *     {
 *         // Reply received
 *     }
 * });
 * </pre>
 * <p>A {@link Session} is the active part of the endpoint, and by calling its API applications can generate
 * events on the connection; conversely {@link Session.Listener} is the passive part of the endpoint, and
 * has callbacks that are invoked when events happen on the connection.</p>
 *
 * @see Session.Listener
 */
public interface Session
{
    /**
     * <p>Sends the given HEADERS {@code frame} to create a new {@link Stream}.</p>
     *
     * @param frame the HEADERS frame containing the HTTP headers
     * @param listener the listener that gets notified of stream events
     * @return a CompletableFuture that is notified of the stream creation
     */
    public default CompletableFuture<Stream> newStream(HeadersFrame frame, Stream.Listener listener)
    {
        return Promise.Completable.with(p -> newStream(frame, p, listener));
    }

    /**
     * <p>Sends the given HEADERS {@code frame} to create a new {@link Stream}.</p>
     *
     * @param frame the HEADERS frame containing the HTTP headers
     * @param promise the promise that gets notified of the stream creation
     * @param listener the listener that gets notified of stream events
     */
    public void newStream(HeadersFrame frame, Promise<Stream> promise, Stream.Listener listener);

    /**
     * <p>Sends the given PRIORITY {@code frame}.</p>
     * <p>If the {@code frame} references a {@code streamId} that does not exist
     * (for example {@code 0}), then a new {@code streamId} will be allocated, to
     * support <em>unused anchor streams</em> that act as parent for other streams.</p>
     *
     * @param frame the PRIORITY frame to send
     * @param callback the callback that gets notified when the frame has been sent
     * @return the new stream id generated by the PRIORITY frame, or the stream id
     * that it is already referencing
     */
    public int priority(PriorityFrame frame, Callback callback);

    /**
     * <p>Sends the given SETTINGS {@code frame} to configure the session.</p>
     *
     * @param frame the SETTINGS frame to send
     * @return a CompletableFuture that is notified when the frame has been sent
     */
    public default CompletableFuture<Void> settings(SettingsFrame frame)
    {
        return Callback.Completable.with(c -> settings(frame, c));
    }

    /**
     * <p>Sends the given SETTINGS {@code frame} to configure the session.</p>
     *
     * @param frame the SETTINGS frame to send
     * @param callback the callback that gets notified when the frame has been sent
     */
    public void settings(SettingsFrame frame, Callback callback);

    /**
     * <p>Sends the given PING {@code frame}.</p>
     * <p>PING frames may be used to test the connection integrity and to measure
     * round-trip time.</p>
     *
     * @param frame the PING frame to send
     * @param callback the callback that gets notified when the frame has been sent
     */
    public void ping(PingFrame frame, Callback callback);

    /**
     * <p>Closes the session by sending a GOAWAY frame with the given error code
     * and payload.</p>
     *
     * @param error the error code
     * @param payload an optional payload (may be null)
     * @param callback the callback that gets notified when the frame has been sent
     * @return true if the frame is being sent, false if the session was already closed
     */
    public boolean close(int error, String payload, Callback callback);

    /**
     * @return whether the session is not open
     */
    public boolean isClosed();

    /**
     * @return whether the push functionality is enabled
     */
    public boolean isPushEnabled();

    /**
     * @return a snapshot of all the streams currently belonging to this session
     */
    public Collection<Stream> getStreams();

    /**
     * <p>Retrieves the stream with the given {@code streamId}.</p>
     *
     * @param streamId the stream id of the stream looked for
     * @return the stream with the given id, or null if no such stream exist
     */
    public Stream getStream(int streamId);

    /**
     * @return the local network address this session is bound to,
     * or {@code null} if this session is not bound to a network address
     * @deprecated use {@link #getLocalSocketAddress()} instead
     */
    @Deprecated
    public InetSocketAddress getLocalAddress();

    /**
     * @return the local network address this session is bound to,
     * or {@code null} if this session is not bound to a network address
     */
    public default SocketAddress getLocalSocketAddress()
    {
        return getLocalAddress();
    }

    /**
     * @return the remote network address this session is connected to,
     * or {@code null} if this session is not connected to a network address
     * @deprecated use {@link #getRemoteSocketAddress()} instead
     */
    @Deprecated
    public InetSocketAddress getRemoteAddress();

    /**
     * @return the remote network address this session is connected to,
     * or {@code null} if this session is not connected to a network address
     */
    public default SocketAddress getRemoteSocketAddress()
    {
        return getRemoteAddress();
    }

    /**
     * <p>Gracefully closes the session, returning a {@code CompletableFuture} that
     * is completed when all the streams currently being processed are completed.</p>
     * <p>Implementation is idempotent, i.e. calling this method a second time
     * or concurrently results in a no-operation.</p>
     *
     * @return a {@code CompletableFuture} that is completed when all the streams are completed
     */
    public CompletableFuture<Void> shutdown();

    /**
     * <p>A {@link Listener} is the passive counterpart of a {@link Session} and
     * receives events happening on an HTTP/2 connection.</p>
     *
     * @see Session
     */
    public interface Listener
    {
        /**
         * <p>Callback method invoked:</p>
         * <ul>
         * <li>for clients, just before the preface is sent, to gather the
         * SETTINGS configuration options the client wants to send to the server;</li>
         * <li>for servers, just after having received the preface, to gather
         * the SETTINGS configuration options the server wants to send to the
         * client.</li>
         * </ul>
         *
         * @param session the session
         * @return a (possibly empty or null) map containing SETTINGS configuration
         * options to send.
         */
        public default Map<Integer, Integer> onPreface(Session session)
        {
            return null;
        }

        /**
         * <p>Callback method invoked when a new stream is being created upon
         * receiving a HEADERS frame representing an HTTP request.</p>
         * <p>Applications should implement this method to process HTTP requests,
         * typically providing an HTTP response via
         * {@link Stream#headers(HeadersFrame, Callback)}.</p>
         * <p>Applications can detect whether request DATA frames will be arriving
         * by testing {@link HeadersFrame#isEndStream()}. If the application is
         * interested in processing the DATA frames, it must demand for DATA
         * frames using {@link Stream#demand()} and return a
         * {@link Stream.Listener} implementation that overrides
         * {@link Stream.Listener#onDataAvailable(Stream)}.</p>
         *
         * @param stream the newly created stream
         * @param frame the HEADERS frame received
         * @return a {@link Stream.Listener} that will be notified of stream events
         */
        public default Stream.Listener onNewStream(Stream stream, HeadersFrame frame)
        {
            return null;
        }

        /**
         * <p>Callback method invoked when a SETTINGS frame has been received.</p>
         *
         * @param session the session
         * @param frame the SETTINGS frame received
         */
        public default void onSettings(Session session, SettingsFrame frame)
        {
        }

        /**
         * <p>Callback method invoked when a PING frame has been received.</p>
         *
         * @param session the session
         * @param frame the PING frame received
         */
        public default void onPing(Session session, PingFrame frame)
        {
        }

        /**
         * <p>Callback method invoked when a RST_STREAM frame has been received for an unknown stream.</p>
         *
         * @param session the session
         * @param frame the RST_STREAM frame received
         * @see Stream.Listener#onReset(Stream, ResetFrame, Callback)
         */
        public default void onReset(Session session, ResetFrame frame)
        {
        }

        /**
         * <p>Callback method invoked when a GOAWAY frame has been received.</p>
         *
         * @param session the session
         * @param frame the GOAWAY frame received
         */
        default void onGoAway(Session session, GoAwayFrame frame)
        {
        }

        /**
         * <p>Callback method invoked when a GOAWAY frame caused the session to be closed.</p>
         *
         * @param session the session
         * @param frame the GOAWAY frame that caused the session to be closed
         * @param callback the callback to notify of the GOAWAY processing
         */
        public default void onClose(Session session, GoAwayFrame frame, Callback callback)
        {
            callback.succeeded();
        }

        /**
         * <p>Callback method invoked when the idle timeout expired.</p>
         *
         * @param session the session
         * @return whether the session should be closed
         */
        public default boolean onIdleTimeout(Session session)
        {
            return true;
        }

        /**
         * <p>Callback method invoked when a failure has been detected for this session.</p>
         *
         * @param session the session
         * @param failure the failure
         * @param callback the callback to notify of failure processing
         */
        public default void onFailure(Session session, Throwable failure, Callback callback)
        {
            callback.succeeded();
        }
    }
}
