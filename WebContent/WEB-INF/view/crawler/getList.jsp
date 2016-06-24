<?xml version="1.0" encoding="UTF-8" standalone="yes"?><%@page contentType="application/xml"%><%@page pageEncoding="UTF-8"%><%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<apiResponse><c:forEach var="item" items="${it.items}">
	<crawlerDefinition><c:forEach var="item2" items="${item.keyValuePair}">
		<${item2.key}>${item2.value}</${item2.key}></c:forEach>
	</crawlerDefinition></c:forEach>
</apiResponse>
