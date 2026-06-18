package com.safiraenergia.mercadospot;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootTest
class BackendEtlMercadoSpotApplicationTests {

	@BeforeAll
	static void loadEnv() {
		
		try {
			// Cargamos el archivo .env en la aplicación principal
			Dotenv dotenv = Dotenv.configure()
			.ignoreIfMissing()
			.load();
	
			dotenv.entries().forEach(entry ->
				System.setProperty(entry.getKey(), entry.getValue())
			);
		} catch (Exception e) {
			// Si no hay .env, usamos valores por defecto
			System.out.println("No .env file found, usgin default test values");
		}
	}

	@Test
	void contextLoads() {
		// En el contexto de Spring debe cargar correctamente
		System.out.println("Test ejecutado correctamente.");
	}

}
