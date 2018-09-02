<#--
Renders a "product" XML to a HTML files.
-->
<@pp.renameOutputFile extension="html" />
<#assign prod = pp.doc.product>
<html>
<head>
  <title>Product: ${prod.name?cap_first}</title>
</head>
<body>
  <h1>${prod.name?cap_first}</h1>
  <p><b>Price:</b> only ${prod.price}
  <p><b>Description:</b><br>
  ${prod.description}
  <hr>
  <a href="index.html">Back...</a>
</body>
</html>
