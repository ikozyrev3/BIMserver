package org.bimserver.servlets;

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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bimserver.BimServer;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.interfaces.objects.SRevision;
import org.bimserver.models.ifc2x3tc1.*;
import org.bimserver.models.log.AccessMethod;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.shared.exceptions.ServerException;
import org.bimserver.shared.exceptions.UserException;
import org.bimserver.shared.interfaces.ServiceInterface;
import org.bimserver.webservices.ServiceMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ODataServlet extends SubServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(ODataServlet.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public ODataServlet(BimServer bimServer, ServletContext servletContext) {
		super(bimServer, servletContext);
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String requestUri = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (requestUri.startsWith(contextPath)) {
			requestUri = requestUri.substring(contextPath.length());
		}
		
		String pathInfo = "";
		if (requestUri.startsWith("/odata")) {
			pathInfo = requestUri.substring("/odata".length());
		}
		
		if (pathInfo == null) {
			pathInfo = "";
		}

		// Set CORS headers
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

		if ("OPTIONS".equals(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		try {
			if (pathInfo.equals("/$metadata")) {
				serveMetadata(response);
			} else if (pathInfo.equals("/Projects") || pathInfo.startsWith("/Projects(")) {
				serveProjects(request, response);
			} else if (pathInfo.equals("/Models") || pathInfo.startsWith("/Models(")) {
				serveModels(request, response);
			} else if (pathInfo.equals("/Elements") || pathInfo.startsWith("/Elements(")) {
				serveElements(request, response);
			} else if (pathInfo.equals("/Properties") || pathInfo.startsWith("/Properties(")) {
				serveProperties(request, response);
			} else {
				serveServiceDocument(response);
			}
		} catch (Exception e) {
			LOGGER.error("Error in OData servlet", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			ObjectNode error = OBJECT_MAPPER.createObjectNode();
			error.put("error", e.getMessage());
			PrintWriter writer = response.getWriter();
			writer.write(error.toString());
		}
	}

	private void serveServiceDocument(HttpServletResponse response) throws IOException {
		ObjectNode serviceDocument = OBJECT_MAPPER.createObjectNode();
		serviceDocument.put("@odata.context", "$metadata");
		
		ArrayNode value = OBJECT_MAPPER.createArrayNode();
		
		ObjectNode projects = OBJECT_MAPPER.createObjectNode();
		projects.put("name", "Projects");
		projects.put("kind", "EntitySet");
		projects.put("url", "Projects");
		value.add(projects);
		
		ObjectNode models = OBJECT_MAPPER.createObjectNode();
		models.put("name", "Models");
		models.put("kind", "EntitySet");
		models.put("url", "Models");
		value.add(models);
		
		ObjectNode elements = OBJECT_MAPPER.createObjectNode();
		elements.put("name", "Elements");
		elements.put("kind", "EntitySet");
		elements.put("url", "Elements");
		value.add(elements);
		
		ObjectNode properties = OBJECT_MAPPER.createObjectNode();
		properties.put("name", "Properties");
		properties.put("kind", "EntitySet");
		properties.put("url", "Properties");
		value.add(properties);
		
		serviceDocument.set("value", value);
		
		PrintWriter writer = response.getWriter();
		writer.write(serviceDocument.toString());
	}

	private void serveMetadata(HttpServletResponse response) throws IOException {
		response.setContentType("application/xml");
		PrintWriter writer = response.getWriter();
		
		writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		writer.write("<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" Version=\"4.0\">\n");
		writer.write("  <edmx:DataServices>\n");
		writer.write("    <Schema xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Namespace=\"BIMserver\">\n");
		writer.write("      <EntityType Name=\"Project\">\n");
		writer.write("        <Key><PropertyRef Name=\"Id\"/></Key>\n");
		writer.write("        <Property Name=\"Id\" Type=\"Edm.Int64\" Nullable=\"false\"/>\n");
		writer.write("        <Property Name=\"Name\" Type=\"Edm.String\"/>\n");
		writer.write("        <Property Name=\"Description\" Type=\"Edm.String\"/>\n");
		writer.write("        <Property Name=\"CreatedDate\" Type=\"Edm.DateTimeOffset\"/>\n");
		writer.write("      </EntityType>\n");
		writer.write("      <EntityType Name=\"Model\">\n");
		writer.write("        <Key><PropertyRef Name=\"Id\"/></Key>\n");
		writer.write("        <Property Name=\"Id\" Type=\"Edm.Int64\" Nullable=\"false\"/>\n");
		writer.write("        <Property Name=\"ProjectId\" Type=\"Edm.Int64\"/>\n");
		writer.write("        <Property Name=\"Comment\" Type=\"Edm.String\"/>\n");
		writer.write("        <Property Name=\"Date\" Type=\"Edm.DateTimeOffset\"/>\n");
		writer.write("      </EntityType>\n");
		writer.write("      <EntityType Name=\"Element\">\n");
		writer.write("        <Key><PropertyRef Name=\"Id\"/></Key>\n");
		writer.write("        <Property Name=\"Id\" Type=\"Edm.Int64\" Nullable=\"false\"/>\n");
		writer.write("        <Property Name=\"Type\" Type=\"Edm.String\"/>\n");
		writer.write("        <Property Name=\"Name\" Type=\"Edm.String\"/>\n");
		writer.write("        <Property Name=\"ModelId\" Type=\"Edm.Int64\"/>\n");
		writer.write("      </EntityType>\n");
		writer.write("      <EntityType Name=\"Property\">\n");
		writer.write("        <Key><PropertyRef Name=\"Id\"/></Key>\n");
		writer.write("        <Property Name=\"Id\" Type=\"Edm.Int64\" Nullable=\"false\"/>\n");
		writer.write("        <Property Name=\"Name\" Type=\"Edm.String\"/>\n");
		writer.write("        <Property Name=\"Value\" Type=\"Edm.String\"/>\n");
		writer.write("        <Property Name=\"ElementId\" Type=\"Edm.Int64\"/>\n");
		writer.write("      </EntityType>\n");
		writer.write("      <EntityContainer Name=\"Container\">\n");
		writer.write("        <EntitySet Name=\"Projects\" EntityType=\"BIMserver.Project\"/>\n");
		writer.write("        <EntitySet Name=\"Models\" EntityType=\"BIMserver.Model\"/>\n");
		writer.write("        <EntitySet Name=\"Elements\" EntityType=\"BIMserver.Element\"/>\n");
		writer.write("        <EntitySet Name=\"Properties\" EntityType=\"BIMserver.Property\"/>\n");
		writer.write("      </EntityContainer>\n");
		writer.write("    </Schema>\n");
		writer.write("  </edmx:DataServices>\n");
		writer.write("</edmx:Edmx>\n");
	}

	private void serveProjects(HttpServletRequest request, HttpServletResponse response) throws IOException, UserException, ServerException {
		ServiceMap serviceMap = getServiceMap(request);
		if (serviceMap == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		ServiceInterface serviceInterface = serviceMap.get(ServiceInterface.class);
		List<SProject> projects = serviceInterface.getAllProjects(false, false);

		ObjectNode result = OBJECT_MAPPER.createObjectNode();
		result.put("@odata.context", "$metadata#Projects");
		
		ArrayNode value = OBJECT_MAPPER.createArrayNode();
		for (SProject project : projects) {
			ObjectNode projectNode = OBJECT_MAPPER.createObjectNode();
			projectNode.put("Id", project.getOid());
			projectNode.put("Name", project.getName());
			projectNode.put("Description", project.getDescription());
			if (project.getCreatedDate() != null) {
				projectNode.put("CreatedDate", project.getCreatedDate().toInstant().toString());
			}
			value.add(projectNode);
		}
		result.set("value", value);

		PrintWriter writer = response.getWriter();
		writer.write(result.toString());
	}

	private void serveModels(HttpServletRequest request, HttpServletResponse response) throws IOException, UserException, ServerException {
		ServiceMap serviceMap = getServiceMap(request);
		if (serviceMap == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		ServiceInterface serviceInterface = serviceMap.get(ServiceInterface.class);
		List<SProject> projects = serviceInterface.getAllProjects(false, false);

		ObjectNode result = OBJECT_MAPPER.createObjectNode();
		result.put("@odata.context", "$metadata#Models");
		
		ArrayNode value = OBJECT_MAPPER.createArrayNode();
		for (SProject project : projects) {
			List<SRevision> revisions = serviceInterface.getAllRevisionsOfProject(project.getOid());
			for (SRevision revision : revisions) {
				ObjectNode modelNode = OBJECT_MAPPER.createObjectNode();
				modelNode.put("Id", revision.getOid());
				modelNode.put("ProjectId", project.getOid());
				modelNode.put("Comment", revision.getComment());
				if (revision.getDate() != null) {
					modelNode.put("Date", revision.getDate().toInstant().toString());
				}
				value.add(modelNode);
			}
		}
		result.set("value", value);

		PrintWriter writer = response.getWriter();
		writer.write(result.toString());
	}

	private void serveElements(HttpServletRequest request, HttpServletResponse response) throws IOException, UserException, ServerException {
		ServiceMap serviceMap = getServiceMap(request);
		if (serviceMap == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		String modelIdParam = request.getParameter("$filter");
		Long modelId = null;
		
		// Parse simple filter like "ModelId eq 123"
		if (modelIdParam != null && modelIdParam.contains("ModelId eq")) {
			try {
				String[] parts = modelIdParam.split("ModelId eq ");
				if (parts.length > 1) {
					modelId = Long.parseLong(parts[1].trim());
				}
			} catch (NumberFormatException e) {
				// Ignore parsing errors
			}
		}

		ObjectNode result = OBJECT_MAPPER.createObjectNode();
		result.put("@odata.context", "$metadata#Elements");
		
		ArrayNode value = OBJECT_MAPPER.createArrayNode();
		
		try {
			ServiceInterface serviceInterface = serviceMap.get(ServiceInterface.class);
			List<SProject> projects = serviceInterface.getAllProjects(false, false);
			
			for (SProject project : projects) {
				List<SRevision> revisions = serviceInterface.getAllRevisionsOfProject(project.getOid());
				for (SRevision revision : revisions) {
					// If modelId filter is specified, only process that model
					if (modelId != null && !modelId.equals(revision.getOid())) {
						continue;
					}
					
					try {
						// Simple approach: just create sample element data for now
						// In a real implementation, you would load the IFC model here
						ObjectNode elementNode = OBJECT_MAPPER.createObjectNode();
						elementNode.put("Id", revision.getOid() * 1000 + 1); // Generate sample ID
						elementNode.put("Type", "IfcWall");
						elementNode.put("Name", "Sample Wall");
						elementNode.put("ModelId", revision.getOid());
						value.add(elementNode);
						
						ObjectNode elementNode2 = OBJECT_MAPPER.createObjectNode();
						elementNode2.put("Id", revision.getOid() * 1000 + 2); // Generate sample ID
						elementNode2.put("Type", "IfcWindow");
						elementNode2.put("Name", "Sample Window");
						elementNode2.put("ModelId", revision.getOid());
						value.add(elementNode2);
						
						// Limit to avoid memory issues
						if (value.size() > 20) {
							break;
						}
					} catch (Exception e) {
						LOGGER.warn("Could not process model " + revision.getOid() + ": " + e.getMessage());
						// Continue with next model
					}
				}
				
				// Limit total elements across all models
				if (value.size() > 20) {
					break;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error retrieving elements", e);
		}
		
		result.set("value", value);

		PrintWriter writer = response.getWriter();
		writer.write(result.toString());
	}

	private void serveProperties(HttpServletRequest request, HttpServletResponse response) throws IOException, UserException, ServerException {
		ServiceMap serviceMap = getServiceMap(request);
		if (serviceMap == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		String elementIdParam = request.getParameter("$filter");
		Long elementId = null;
		
		// Parse simple filter like "ElementId eq 123"
		if (elementIdParam != null && elementIdParam.contains("ElementId eq")) {
			try {
				String[] parts = elementIdParam.split("ElementId eq ");
				if (parts.length > 1) {
					elementId = Long.parseLong(parts[1].trim());
				}
			} catch (NumberFormatException e) {
				// Ignore parsing errors
			}
		}

		ObjectNode result = OBJECT_MAPPER.createObjectNode();
		result.put("@odata.context", "$metadata#Properties");
		
		ArrayNode value = OBJECT_MAPPER.createArrayNode();
		
		try {
			ServiceInterface serviceInterface = serviceMap.get(ServiceInterface.class);
			List<SProject> projects = serviceInterface.getAllProjects(false, false);
			
			for (SProject project : projects) {
				List<SRevision> revisions = serviceInterface.getAllRevisionsOfProject(project.getOid());
				for (SRevision revision : revisions) {
					try {
						// Simple approach: create sample property data
						// In a real implementation, you would load the IFC model and extract properties
						long sampleElementId1 = revision.getOid() * 1000 + 1;
						long sampleElementId2 = revision.getOid() * 1000 + 2;
						
						// If elementId filter is specified, only process that element
						if (elementId == null || elementId.equals(sampleElementId1) || elementId.equals(sampleElementId2)) {
							ObjectNode propertyNode1 = OBJECT_MAPPER.createObjectNode();
							propertyNode1.put("Id", revision.getOid() * 10000 + 1);
							propertyNode1.put("Name", "Height");
							propertyNode1.put("Value", "3000");
							propertyNode1.put("ElementId", sampleElementId1);
							value.add(propertyNode1);
							
							ObjectNode propertyNode2 = OBJECT_MAPPER.createObjectNode();
							propertyNode2.put("Id", revision.getOid() * 10000 + 2);
							propertyNode2.put("Name", "Material");
							propertyNode2.put("Value", "Concrete");
							propertyNode2.put("ElementId", sampleElementId1);
							value.add(propertyNode2);
							
							ObjectNode propertyNode3 = OBJECT_MAPPER.createObjectNode();
							propertyNode3.put("Id", revision.getOid() * 10000 + 3);
							propertyNode3.put("Name", "Width");
							propertyNode3.put("Value", "1200");
							propertyNode3.put("ElementId", sampleElementId2);
							value.add(propertyNode3);
						}
						
						// Limit to avoid memory issues
						if (value.size() > 50) {
							break;
						}
					} catch (Exception e) {
						LOGGER.warn("Could not process model " + revision.getOid() + ": " + e.getMessage());
						// Continue with next model
					}
				}
				
				// Limit total properties across all models
				if (value.size() > 50) {
					break;
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error retrieving properties", e);
		}
		
		result.set("value", value);

		PrintWriter writer = response.getWriter();
		writer.write(result.toString());
	}

	private ServiceMap getServiceMap(HttpServletRequest request) throws UserException {
		String token = request.getHeader("Authorization");
		if (token != null && token.startsWith("Bearer ")) {
			token = token.substring(7);
		}
		if (token == null) {
			token = request.getParameter("token");
		}
		if (token == null) {
			return null;
		}
		return getBimServer().getServiceFactory().get(token, AccessMethod.JSON);
	}
}