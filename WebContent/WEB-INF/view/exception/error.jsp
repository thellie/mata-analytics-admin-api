<?xml version="1.0" encoding="UTF-8" standalone="yes"?><%@page contentType="application/xml"%><%@page pageEncoding="UTF-8"%><%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<apiResponse>
	<code>500</code>
	<message><c:forEach var="item" items="${it.items}">${item.message}</c:forEach></message>
	<detail><c:forEach var="item" items="${it.items}">${item.detail}</c:forEach></detail>
	<value>1</value>
</apiResponse>
