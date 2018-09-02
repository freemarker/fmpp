<#--
Renders a "book" XML to multiple HTML files.
This is a rather complex stuff... check the product.html first.
-->

<@pp.dropOutputFile />

<#-- Calculating the prefix of output file names -->
<#assign prefix = pp.sourceFileName>
<#assign prefix = prefix?substring(0, prefix?last_index_of('.'))>
<#assign prefix = prefix + '_'>

<#-- Calculate the ordered list of HTML pages, used for the nav. bar. -->
<#assign pages = pp.doc["//*[@ppFile != '']"]>

<#-- Render the HTML pages -->
<#recurse pp.doc>


<#--
  ***********************************************************************************
  Node handlers
  ***********************************************************************************
-->

<#macro book>
  <@pp.nestOutputFile name="${prefix}index.html">
    <@Html title="${.node.@title}">
      <p><b>Table of Contents</b></p>
      <ul>
      <#list .node.part as part>
        <li><a href="${prefix}${part.@ppFile}">${part.@title}</a>
        <#local cs = part.chapter>
        <#if cs?has_content>
          <ul>
          <#list cs as chapter>
            <li><a href="${prefix}${chapter.@ppFile}">${chapter.@title}</a>
          </#list>
          </ul>
        </#if>
      </#list>
      </ul>
      <#recurse>
    </@Html>
  </@pp.nestOutputFile>
</#macro>

<#macro part>
  <@pp.nestOutputFile name=prefix+.node.@ppFile>
    <@Html title="${.node.@title}">
      <#local cs = .node.chapter>
      <#if cs?has_content>
        <p><b>Part Contents</b></p>
        <ul>
        <#list cs as chapter>
          <li><a href="${prefix}${chapter.@ppFile}">${chapter.@title}</a>
        </#list>
        </ul>
      </#if>
      <#recurse>
    </@Html>
  </@pp.nestOutputFile>
</#macro>

<#macro chapter>
  <@pp.nestOutputFile name=prefix+.node.@ppFile>
    <@Html title="${.node.@title}">
      <#recurse>
    </@Html>
  </@pp.nestOutputFile>
</#macro>

<#macro p>
  <p><#recurse></p>
</#macro>

<#macro quote>
  <blockquote><i><#recurse></i></blockquote>
</#macro>

<#macro e>
  <b><#recurse></b><#t>
</#macro>

<#macro mark>
  <span style="background-color: yellow"><#recurse></span><#t>
</#macro>

<#macro @text>
  ${.node}<#t>
</#macro>


<#--
  ***********************************************************************************
  Other macros
  ***********************************************************************************
-->

<#macro Html title>
  <html>
    <head>
      <title>${title}</title>
    </head>
    <body>
      <@NavBar />
      <hr>
      <h1>${title}</h1>
      <#nested>
      <hr>
      <@NavBar />
    </body>
  </html>
</#macro>

<#macro NavBar>
  <#local nodeName = .node?node_name>
  <#if nodeName == 'book'>
    Prev | Up | <a href="${prefix}${pages[0].@ppFile}">Next</a> | ToC | <a href="index.html">Home</a>
  <#else>
    <#local ppFile = .node.@ppFile>
    <#local max = pages?size - 1>
    <#list 0..max as i>
      <#if pages[i].@ppFile == ppFile>
        <#local idx = i>
        <#break>
      </#if>
    </#list>
    <#if idx != 0>
      <a href="${prefix}${pages[idx - 1].@ppFile}">Prev</a><#t>
    <#else>
      <a href="${prefix}index.html">Prev</a><#t>
    </#if>
    |
    <#list .node?ancestors as a>
      <#if a.@ppFile[0]??>
        <a href="${prefix}${a.@ppFile}">Up</a><#t>
        <#break>
      <#elseif a?node_name == 'book'>
        <a href="${prefix}index.html">Up</a><#t>
        <#break>
      </#if>
    </#list>
    |
    <#if idx != max>
      <a href="${prefix}${pages[idx + 1].@ppFile}">Next</a>
    <#else>
      Next<#t>
    </#if>
    |
    <a href="${prefix}index.html">ToC</a> | <a href="index.html">Home</a>
  </#if>
</#macro>
