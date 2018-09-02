<#assign h1Counter = 0>
<#assign contents = pp.newWritableSequence()>

<#macro h1>
  <#local title><#nested></#local>
  <#assign h1Counter = h1Counter + 1>
  <@pp.add seq=contents value={"anchor":h1Counter, "title":title} />
  <a name="${h1Counter}"></a>
  <h1>${title}</h1>
</#macro>

<#macro page>
  <#local output>
    <#nested>
  </#local>
  <b>Contents:</b>
  <ul>
  <#list contents as h>
    <li><a href="#${h.anchor}">${h.title}</a>
  </#list>
  </ul>
  <hr>
  ${output}
</#macro>