<#-- $Id: page.ftl,v 1.1 2003/10/11 11:29:23 ddekany Exp $ -->
<#macro page title>
<#escape x as x?html>
<head>
  <title>${title?cap_first}</title>
</head>
<body bgcolor="${backgroundColor}" text="${textColor}">
  <h1>${title?cap_first}</h1>
  <#nested>
</body>
</#escape>
</#macro>