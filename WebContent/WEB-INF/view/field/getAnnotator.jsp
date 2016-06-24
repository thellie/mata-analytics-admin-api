<?xml version="1.0" encoding="UTF-8" standalone="yes"?><%@page contentType="application/xml"%><%@page pageEncoding="UTF-8"%><%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<apiResponse><c:forEach var="item" items="${it.items}">
	<annotator>
		<name>${item.name}</name>
		<feature>
		<c:forEach var="item2" items="${item.features}">
			<featurename>
				${item2}
			</featurename>
		</c:forEach>
		</feature>
	</annotator></c:forEach>
</apiResponse>