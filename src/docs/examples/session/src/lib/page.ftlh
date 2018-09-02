<#macro page title>
  <#if !pp.s.contents??>
    <@pp.set hash=pp.s key="contents" value=pp.newWritableSequence() />
  </#if>
  <@pp.add seq=pp.s.contents value={"link":pp.outputFile, "title":title} />
  <html>
    <head><title>${title}</title></head>
  </html>
  <body>
    <h1>${title}</h1>
    <#nested>
  </body>
</#macro>
