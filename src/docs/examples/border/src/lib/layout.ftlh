<#macro title><#assign captured_title><#nested></#assign></#macro>

<#macro body><#assign captured_body><#nested></#assign></#macro>

<#macro page>
  <#nested>
  <html>
    <head>
      <title>${captured_title}</title>
    </head>
    <body>
      <h1>${captured_title}</h1>
      ${captured_body}
    </body>
  </html>
</#macro>
