# BIMserver OData Protocol

This document describes the OData protocol implementation for BIMserver, providing access to Projects, Models, Elements, and Properties.

## Overview

The OData protocol provides a RESTful API for accessing BIMserver data in a standardized way. It supports standard OData operations and query syntax.

## Base URL

All OData endpoints are available under the `/odata` path:
```
http://your-bimserver-host/odata
```

## Authentication

Authentication is required for all OData endpoints. You can authenticate using:

1. **Bearer Token**: Include in the Authorization header
   ```
   Authorization: Bearer your-token
   ```

2. **Query Parameter**: Include as a URL parameter
   ```
   /odata/Projects?token=your-token
   ```

## Available Entity Sets

### 1. Projects
Access project information.

**Endpoint**: `/odata/Projects`

**Properties**:
- `Id` (Int64): Project identifier
- `Name` (String): Project name
- `Description` (String): Project description
- `CreatedDate` (DateTimeOffset): Creation date

**Example**:
```
GET /odata/Projects
```

### 2. Models
Access model/revision information.

**Endpoint**: `/odata/Models`

**Properties**:
- `Id` (Int64): Model identifier
- `ProjectId` (Int64): Associated project ID
- `Comment` (String): Revision comment
- `Date` (DateTimeOffset): Revision date

**Example**:
```
GET /odata/Models
```

### 3. Elements
Access IFC elements within models.

**Endpoint**: `/odata/Elements`

**Properties**:
- `Id` (Int64): Element identifier
- `Type` (String): IFC element type (e.g., IfcWall, IfcWindow)
- `Name` (String): Element name
- `ModelId` (Int64): Associated model ID

**Examples**:
```
GET /odata/Elements
GET /odata/Elements?$filter=ModelId eq 123
```

### 4. Properties
Access properties of IFC elements.

**Endpoint**: `/odata/Properties`

**Properties**:
- `Id` (Int64): Property identifier
- `Name` (String): Property name
- `Value` (String): Property value
- `ElementId` (Int64): Associated element ID

**Examples**:
```
GET /odata/Properties
GET /odata/Properties?$filter=ElementId eq 456
```

## OData Standard Operations

### Service Document
Get information about available entity sets:
```
GET /odata
```

### Metadata Document
Get the data model schema:
```
GET /odata/$metadata
```

### Filtering
Use OData `$filter` query option to filter results:
```
GET /odata/Elements?$filter=ModelId eq 123
GET /odata/Properties?$filter=ElementId eq 456
```

## Response Format

All responses are in JSON format with OData-compliant structure:

```json
{
  "@odata.context": "$metadata#EntitySetName",
  "value": [
    {
      "Id": 1,
      "Name": "Example",
      ...
    }
  ]
}
```

## Error Handling

Errors are returned with appropriate HTTP status codes:
- `401 Unauthorized`: Authentication required
- `500 Internal Server Error`: Server error

Error responses include error details in JSON format:
```json
{
  "error": "Error message"
}
```

## CORS Support

The OData endpoints support Cross-Origin Resource Sharing (CORS) to enable access from web applications.

## Limitations

- Element and Property endpoints currently return sample data
- Full IFC model loading and element extraction will be implemented in future versions
- Performance optimizations are applied (limited to 20 elements, 50 properties per request)

## Future Enhancements

- Full IFC model integration
- More advanced OData query operations ($select, $expand, $orderby, $top, $skip)
- Real-time element and property extraction from IFC models
- Performance improvements for large models