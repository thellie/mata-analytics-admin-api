<?xml version="1.0" encoding="UTF-8" standalone="yes"?><%@page contentType="application/xml"%><%@page pageEncoding="UTF-8"%><%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<apiResponse><c:forEach var="item" items="${it.items}">
	<tokenDefinition>
		<type>${item.type}</type>
		<token>${item.token}</token>
		<alias>${item.alias}</alias>
		<keyid>${item.keyid}</keyid>
		<secret>${item.secret}</secret>
	</tokenDefinition></c:forEach>
</apiResponse>
