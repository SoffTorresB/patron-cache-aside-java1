package com.ejemplo.service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import com.google.gson.Gson;
import com.ejemplo.model.Empleado;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ServicioCache {

	// DATOS DE LA BASE
	private static final String MYSQL_URL = "jdbc:mysql://localhost:3309/employees";
	private static final String MYSQL_USER = "root";
	private static final String MYSQL_PASSWORD = "rootroot";
	private static JedisPool jedisPool = new JedisPool("localhost", 6379);
	private static Gson gson = new Gson();

	public static void main(String[] args) {
		int idEmpleadoBuscar = 10001;

		System.out.println("=== PROBANDO ESTRATEGIA CACHE-ASIDE (REDIS + MYSQL DOCKER) ===");

		System.out.println("\n--- CONSULTA 1 (Debería ser CACHE MISS: Busca en el MySQL de Docker) ---");
		// Empleado emp1 = obtenerEmpleado(idEmpleadoBuscar);
		// System.out.println("Resultado 1: " + emp1);

		// --- PRUEBA PARA CONSULTA 1 (En Frío / Cache Miss) ---
		long tiempoInicio1 = System.nanoTime();

		Empleado emp1 = obtenerEmpleado(10001);

		long tiempoFin1 = System.nanoTime();
		double tiempoMilisegundos1 = (tiempoFin1 - tiempoInicio1) / 1_000_000.0;
		System.out.println("Tiempo de ejecución Consulta 1: " + tiempoMilisegundos1 + " ms");

		System.out.println("\n--- CONSULTA 2 (Debería ser CACHE HIT: Lo extrae de Redis en memoria RAM) ---");
		// Empleado emp2 = obtenerEmpleado(idEmpleadoBuscar);
		// System.out.println("Resultado 2: " + emp2);

		// --- PRUEBA PARA CONSULTA 2 (En Caliente / Cache Hit de Redis) ---
		long tiempoInicio2 = System.nanoTime();

		Empleado emp2 = obtenerEmpleado(10001);

		long tiempoFin2 = System.nanoTime();
		double tiempoMilisegundos2 = (tiempoFin2 - tiempoInicio2) / 1_000_000.0;
		System.out.println("Tiempo de ejecución Consulta 2: " + tiempoMilisegundos2 + " ms");

		jedisPool.close();
	}

	public static Empleado obtenerEmpleado(int empNo) {
		String redisKey = "empleado:" + empNo;
		try (Jedis jedis = jedisPool.getResource()) {

			/*
			 * // VERIFICACIÓN 1: ¿Está guardado en la memoria RAM de Redis? String
			 * jsonCache = jedis.get(redisKey);
			 * 
			 * if (jsonCache != null) {
			 * System.out.println(" -> [CACHE HIT] ¡Dato encontrado en memoria RAM (Redis)!"
			 * ); return gson.fromJson(jsonCache, Empleado.class); }
			 */

			// PROCEDIMIENTO DE RESPALDO: Cache Miss
			System.out.println(" -> [CACHE MISS] No está en memoria. Conectando al contenedor MySQL...");
			Empleado empleadoBD = consultarMySQL(empNo);

			if (empleadoBD != null) {
				System.out.println(" -> Registro encontrado en MySQL. Guardando copia en Redis...");
				String jsonParaGuardar = gson.toJson(empleadoBD);

				jedis.setex(redisKey, 300, jsonParaGuardar);

				return empleadoBD;
			} else {
				System.out.println(" -> [ALERTA] El empleado con ID " + empNo + " no existe en MySQL.");
			}

		} catch (Exception e) {
			System.err.println("Error en la infraestructura de datos: " + e.getMessage());
		}

		return null;
	}

//CONSULTA:
	private static Empleado consultarMySQL(int empNo) {
		String query = "SELECT emp_no, first_name, last_name, gender FROM employees WHERE emp_no = ?";

		try (Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
				PreparedStatement stmt = conn.prepareStatement(query)) {

			stmt.setInt(1, empNo);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return new Empleado(rs.getInt("emp_no"), rs.getString("first_name"), rs.getString("last_name"),
							rs.getString("gender"));
				}
			}
		} catch (SQLException e) {
			System.err.println("Error al ejecutar Query en MySQL Docker: " + e.getMessage());
		}
		return null;
	}
}