package com.jets.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketConnectionTest {

	@LocalServerPort
	private int port;

	@Test
	void clientCanConnectToWebSocketEndpoint() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		StandardWebSocketClient client = new StandardWebSocketClient();

		WebSocketSession session = client.execute(
				new TextWebSocketHandler() {
					@Override
					public void afterConnectionEstablished(WebSocketSession s) {
						latch.countDown();
					}
				},
				"ws://localhost:" + port + "/ws/game?playerName=TestPlayer"
		).get(3, TimeUnit.SECONDS);

		assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
		assertThat(session.isOpen()).isTrue();
		session.close();
	}
}
