<?xml version="1.0" encoding="UTF-8" standalone="yes"?><%@page contentType="application/xml"%><%@page pageEncoding="UTF-8"%><%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<apiResponse><c:forEach var="item" items="${it.items}">
	<annotator>
		<name>${item.name}</name>
		<mapping>
		<c:forEach var="item2" items="${item.mapping}">
			<featurename>
				${item2.featurename}
			</featurename>
			<fieldname>
				${item2.fieldname}
			</fieldname>
		</c:forEach>
		</mapping>
	</annotator></c:forEach>
</apiResponse>