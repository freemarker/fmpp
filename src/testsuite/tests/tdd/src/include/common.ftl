<#-- $Id: common.ftl,v 1.2 2007/04/27 12:45:28 ddekany Exp $ -->

<#macro dumpTopLevel m>
  <#list m?keys?sort as k>
    - (${k}) : <@dump m[k] />
  </#list>
</#macro>

<#macro dump v>
  <#if v?is_boolean>
    bool (${v?string})<#t>
  <#elseif v?is_number>
    num (${v})<#t>
  <#elseif v?is_sequence>
    seq [<#list v as x>(<@dump x />)<#if x_has_next>, </#if></#list>]<#t>
  <#elseif v?is_string>
    str (${v})<#t>
  <#elseif v?is_hash>
    hash {<#list v?keys?sort as k>(${k}):(<@dump v[k] />)]<#if k_has_next>, </#if></#list>}<#t>
  </#if>
</#macro>