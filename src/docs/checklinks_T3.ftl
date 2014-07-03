<@pp.dropOutputFile/>

<#list pp.s.hrefs as href>
  <#if !pp.outputFileExists(href.file)>
    <@pp.warning message='Broken link "${href.href}" in "${href.source}": target file not found.' />
  <#else>
    <#if href.anchor != '' && !href.file?starts_with('freemarker/') && !href.file?starts_with('api/')>
      <#if !pp.s.anchors[href.file + '#' + href.anchor]??>
        <@pp.warning message='Broken link "${href.href}" in "${href.source}": target anchor not found in the target file.' />
      </#if>
    </#if>
  </#if>
</#list>