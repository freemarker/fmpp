<#macro a href>
  <#-- remove anchor link ending -->
  <#local x = href?index_of('#')>
  <#if x != -1>
    <#local href = href?substring(0, x)>
  </#if>
  <#-- check existence -->
  <#if !pp.sourceFileExists(href)>
    <@pp.warning message="Broken link in " + pp.sourceFile + ": " + href />
  </#if>
  <#-- print HTML -->
  <a href="${href}"><#nested></a><#t>
</#macro>