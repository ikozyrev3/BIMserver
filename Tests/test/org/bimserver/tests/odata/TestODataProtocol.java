package org.bimserver.tests.odata;

/******************************************************************************
 * Copyright (C) 2009-2019  BIMserver.org
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see {@literal<http://www.gnu.org/licenses/>}.
 *****************************************************************************/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.bimserver.LocalDevBimServerStarter;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.shared.PublicInterfaceNotFoundException;
import org.bimserver.shared.exceptions.ServerException;
import org.bimserver.shared.exceptions.ServiceException;
import org.bimserver.shared.exceptions.UserException;
import org.bimserver.tests.utils.TestWithEmbeddedServer;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.*;

public class TestODataProtocol extends TestWithEmbeddedServer {

	@Test
	public void testODataServiceDocument() throws Exception {
		try (BimServerClientInterface client = getFactory().create()) {
			// Login first
			client.getAuthInterface().login("admin@bimserver.org", "admin");
			
			// Test service document
			String response = makeODataRequest("/odata");
			assertNotNull("Response should not be null", response);
			
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(response);
			assertTrue("Should contain value array", root.has("value"));
			
			JsonNode value = root.get("value");
			assertTrue("Value should be an array", value.isArray());
			assertTrue("Should have at least one entity set", value.size() > 0);
			
			// Check if we have our expected entity sets
			boolean hasProjects = false, hasModels = false, hasElements = false, hasProperties = false;
			for (JsonNode entitySet : value) {
				String name = entitySet.get("name").asText();
				if ("Projects".equals(name)) hasProjects = true;
				if ("Models".equals(name)) hasModels = true;
				if ("Elements".equals(name)) hasElements = true;
				if ("Properties".equals(name)) hasProperties = true;
			}
			
			assertTrue("Should have Projects entity set", hasProjects);
			assertTrue("Should have Models entity set", hasModels);
			assertTrue("Should have Elements entity set", hasElements);
			assertTrue("Should have Properties entity set", hasProperties);
		}
	}

	@Test
	public void testODataMetadata() throws Exception {
		try (BimServerClientInterface client = getFactory().create()) {
			// Login first
			client.getAuthInterface().login("admin@bimserver.org", "admin");
			
			// Test metadata document
			String response = makeODataRequest("/odata/$metadata");
			assertNotNull("Response should not be null", response);
			
			assertTrue("Should be XML response", response.contains("<?xml"));
			assertTrue("Should contain EDMX", response.contains("edmx:Edmx"));
			assertTrue("Should contain Project entity", response.contains("EntityType Name=\"Project\""));
			assertTrue("Should contain Model entity", response.contains("EntityType Name=\"Model\""));
			assertTrue("Should contain Element entity", response.contains("EntityType Name=\"Element\""));
			assertTrue("Should contain Property entity", response.contains("EntityType Name=\"Property\""));
		}
	}

	@Test 
	public void testODataProjects() throws Exception {
		try (BimServerClientInterface client = getFactory().create()) {
			// Login first
			client.getAuthInterface().login("admin@bimserver.org", "admin");
			
			// Test projects endpoint
			String response = makeODataRequest("/odata/Projects");
			assertNotNull("Response should not be null", response);
			
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(response);
			assertTrue("Should contain @odata.context", root.has("@odata.context"));
			assertTrue("Should contain value array", root.has("value"));
			
			String context = root.get("@odata.context").asText();
			assertTrue("Context should reference Projects", context.contains("Projects"));
		}
	}

	@Test
	public void testODataModels() throws Exception {
		try (BimServerClientInterface client = getFactory().create()) {
			// Login first
			client.getAuthInterface().login("admin@bimserver.org", "admin");
			
			// Test models endpoint
			String response = makeODataRequest("/odata/Models");
			assertNotNull("Response should not be null", response);
			
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(response);
			assertTrue("Should contain @odata.context", root.has("@odata.context"));
			assertTrue("Should contain value array", root.has("value"));
			
			String context = root.get("@odata.context").asText();
			assertTrue("Context should reference Models", context.contains("Models"));
		}
	}

	private String makeODataRequest(String path) throws IOException {
		URL url = new URL("http://localhost:" + LocalDevBimServerStarter.PORT + path);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", "application/json");
		
		int responseCode = connection.getResponseCode();
		if (responseCode == 401) {
			// For unauthorized, we may need authentication token
			// For now, we'll return the error response for testing
		}
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		StringBuilder response = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			response.append(line);
		}
		reader.close();
		
		return response.toString();
	}
}