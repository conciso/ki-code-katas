package com.jets.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Client {

	private String id;
	private String name;
	private boolean ready;
	private String color;

	public Client(String name) {
		this.name = name;
	}

	public Client(String id, String name, String color) {
		this.id = id;
		this.name = name;
		this.color = color;
	}
}
