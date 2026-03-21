package com.jets.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.jets.backend.model.Client;

class ClientTest {

	@Test
	void defaultClientReadyIsFalse() {
		Client client = new Client();

		assertThat(client.isReady()).isFalse();
	}

	@Test
	void defaultClientNameIsNull() {
		Client client = new Client();

		assertThat(client.getName()).isNull();
	}

	@Test
	void clientConstructorWithNameSetsName() {
		Client client = new Client("Alice");

		assertThat(client.getName()).isEqualTo("Alice");
	}

	@Test
	void clientReadyIsInitiallyFalseAfterNamedConstruction() {
		Client client = new Client("Alice");

		assertThat(client.isReady()).isFalse();
	}

	@Test
	void clientReadyCanBeSetToTrue() {
		Client client = new Client("Alice");

		client.setReady(true);

		assertThat(client.isReady()).isTrue();
	}

	@Test
	void clientReadyCanBeSetToFalse() {
		Client client = new Client("Alice");
		client.setReady(true);

		client.setReady(false);

		assertThat(client.isReady()).isFalse();
	}
}
