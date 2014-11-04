<@pp.ignoreOutput>
<#escape x as x?html>

<#assign P_titleColor = "#000000">
<#assign P_font = "Arial, sans-serif">

<#include "page.ftl">

<#macro figure src alt>
  <#local label><#nested></#local>
  <#if label?length != 0>
    <p align=center>
  </#if>
  <@html.img src="${pp.home}figures/${src}" alt=alt /><#t>
  <#if label?length != 0>
    <br><#lt>
    <br><strong>Figure:</strong> <em><#noescape>${label}</#noescape></em>
    </p>
  </#if>
</#macro>

<#macro sect title anchor=''>
  <#assign P_sectLevel = P_sectLevel + 1>
  <#assign P_sectId = P_sectId + 1>
  <#if anchor = ''>
    <#local anchor = "sect" + P_sectId>
  </#if>
  <#if P_sectLevel = 1>
     <@pp.add seq=P_sects value={"title":title, "id":anchor} />
     <@.namespace.anchor anchor />
     <h2><#rt>
     <#if title?starts_with('ex:')>
       <@title[3..(title?length-1)]?interpret /><#t>
     <#else>
       ${title}<#t>
     </#if>
     </h2><#lt>
     <#nested>
     <@pp.clear seq=P_settings_in_context />
  <#elseif P_sectLevel = 2>
     <@.namespace.anchor anchor />
     <h3><#rt>
     <#if title?starts_with('ex:')>
       <@title[3..(title?length-1)]?interpret /><#t>
     <#else>
       ${title}<#t>
     </#if>
     </h3><#lt>
     <#nested>
  <#else>
    <#stop "@sect is nested too deeply.">
  </#if>
  <#assign P_sectLevel = P_sectLevel - 1>
</#macro>

<#macro anchor id>
  <#if id != ''>
    <#if !pp.s.anchors??>
      <@pp.set hash=pp.s key="anchors" value=pp.newWritableHash() />
    </#if>
    <@pp.set hash=pp.s.anchors key='${pp.outputFile}#${id}' value=true />
    <a name="${id}"></a><#lt>
  </#if>
</#macro>

<#-- Draws a 100% width horizontal line with the given color. Do not use inside another table (IE 5)... -->
<#macro hr>
  <hr>
</#macro>

<#macro c><code><#nested></code></#macro>

<#macro r><em><#nested></em></#macro>

<#macro e><strong><#nested></strong></#macro>

<#macro note><p><em><strong>Note:</strong> <#nested></em></p></#macro>

<#macro s check=true>
  <#local name>
    <#nested>
  </#local>
  <#if check && !stdSettings[name]??>
    <#stop 'This is not a standard setting: ${name} ' />
  </#if>
  <#if check>
    <#local link = true>
    <#list P_settings_in_context as s>
      <#if s = name>
        <#local link = false>
        <#break>
      </#if>
    </#list>
  <#else>
    <#local link = false>
  </#if>
  <code><#if link><a href="${pp.pathTo('/settings.html')}#key_${name}"></#if><#t>
    ${name}<#t>
  <#if link></a></#if></code><#t>
</#macro>

<#macro prg escape=true>
  <#local body><#nested></#local>
  <pre><#t>
    <#if !escape>
      <#noescape>${body?chop_linebreak}</#noescape><#t>
    <#else>
      ${body?chop_linebreak}<#t>
    </#if>
  </pre><#t>
</#macro>

<#macro toc title=''>
  <#if title != ''>
    <p class="toc-header">${title}</p>
  </#if>
  <ul class="table-of-contents">
    <#nested>
  </ul>
  <div class="clear"></div>
</#macro>

<#macro toci href title>
  <#if href=''>
    <li>${title}<#nested></li>
  <#else>
    <li><a href="${href}">${title}</a><#nested></li>
  </#if>
</#macro>

<#macro setting name type default merging clShort='' deprecated='' antAltAtt=''>
  <#if !stdSettings[name]??>
    <#stop 'No such standard setting exists: ${name}'>
  </#if>
  <@_index name />
  <@pp.add seq=P_settings_in_context value=name />
  <p>
    <em>Name:</em> <strong>${name}</strong><br>
    <#if deprecated?length != 0>
      <em class="warning">Deprecated!</em> <@deprecated?interpret /><br>
    </#if>
    <em>Type:</em> ${type}<br>
    <em>Default:</em> <#rt>
    <#if default != ''>
      <#if default?starts_with('ex:')>
        <@default[3..(default?length - 1)]?interpret /><br><#lt>
      <#else>
        <@c>${default}</@><br><#lt>
      </#if>
    <#else>
      No default value<br><#lt>
    </#if>
    <#if !(type?starts_with('string') || type?starts_with('integer') || type?starts_with('boolean'))>
      <#if !(type?starts_with('sequence') || type?starts_with('hash'))>
        <#stop "Unknown setting type: ${type}">
      </#if>
      <#--
      <i>Merging: ${merging?string('yes', 'no')}</i><br><#lt>
      -->
      <#if !merging>
        <#stop "The \"Merging\" field was hidden because all collection-like types "
            + "were merged till now. If it has changed, you have to review the "
            + "documentation and this part of site.ftl. The problematic setting was: "
            + name>
      </#if>
    </#if>
    <#if clShort != ''>
      <em>Command-line short name: </em><code>${clShort}</code><br><#lt>
    </#if>
    <#if antAltAtt != ''>
      <em>Ant task attribute name alternative: </em><code>${antAltAtt}</code><br><#lt>
    </#if>
  </p>
</#macro>

<#macro variable name type nestedContent=false result='' anchor='' deprecated=''>
  <@_checkType type />
  <@_index name />
  <@sect title=name anchor=anchor>
    <#if deprecated != ''>
      <strong class="warning"><em>Deprecated:</em></strong> <@deprecated?interpret /><br>
    </#if>
    <em>Type:</em> ${type}
    <#assign P_params = pp.newWritableSequence()>
    <#local body><#nested></#local>
    <#if (type = 'directive' || type = 'method')>
        <#if type="directive">
          <br><em>Supports nested content:</em> ${nestedContent?string("yes", "no")}
        <#else>
          <br><em>Result type:</em> ${result}
        </#if>
      <br><em>Parameters:</em>
      <#if P_params?size != 0>
          <#if type="directive">
          <ul>
            <#list P_params as param>
              <li><strong>${param.name}</strong><#if param.default != ''>=${param.default}</#if>:
                ${param.type}.
              <#noescape>${param.desc}</#noescape>
            </#list>
          </ul>
         <#else>
          <ol>
            <#list P_params as param>
            <li><strong>${param.name}</strong>: ${param.type}<#if param.optional>, optional</#if>.
              <#noescape>${param.desc}</#noescape>
            </#list>
            </ol>
        </#if>
      <#else>
        none
      </#if>
    </#if>
    <#noescape>${body}</#noescape>
  </@sect>
</#macro>

<#macro dataLoader name>
  <@_index name=name />
  <@sect title=name>
    <#assign P_params = pp.newWritableSequence()>
    <#local body><#nested></#local>
  <em>Parameters:</em>
  <#if P_params?size != 0>
      <ol>
        <#list P_params as param>
        <li><strong>${param.name}</strong>: ${param.type}<#if param.optional>, optional</#if>.
          <#noescape>${param.desc}</#noescape>
        </#list>
      </ol>
  <#else>
    none<br>
  </#if>
    <#noescape>${body}</#noescape>
  </@sect>
</#macro>

<#macro param name type optional=false default=''>
  <#local body><#nested></#local>
  <@pp.add seq=P_params value={
      "name":name, "type":type, "desc":body,  <#-- common -->
      "optional":optional, <#-- methods only -->
      "default":default}  <#-- directives only -->
  />
</#macro>

<#macro _index name>
  <#if !P_indexId??>
    <#assign P_indexId = 1>
  <#else>
    <#assign P_indexId = P_indexId + 1>
  </#if>
  <#assign id = "key_" + name?replace(' ', '_')?replace('.', '_')>
  <@pp.add seq=P_index value={"id":id, "title":name}/>
  <@anchor id />
</#macro>

<#macro _checkType type>
  <#if !(type = 'directive' || type = 'method' || type = 'string' || type = 'number' || type='boolean'
      || type = 'hash' || type = 'sequence' || type = 'writable hash' || type = 'writable sequence'
      || type = 'date' || type = 'date (date only)' || type = 'date (time only)'
      || type = 'date (date+time)'
      || type = 'any' || type = 'node')
  >
    <#stop 'Bad variable type: ' + type>
  </#if>
</#macro>

<#macro a href rel="">
  <a href="${href}"<#if rel?has_content> rel="${rel}"</#if>><#nested></a><#t>
  <#if !pp.s.hrefs??>
    <@pp.set hash=pp.s key="hrefs" value=pp.newWritableSequence()/>
  </#if>
  <#local x = href?index_of('#')>
  <#if x != -1>
    <#if x != 0>
      <#local file = href[0..x-1]>
    <#else>
      <#local file = ''>
    </#if>
    <#local anchor = href[(x+1)..(href?length - 1)]>
    <#if file?length = 0>
      <#local file = pp.outputFileName>
    </#if>
  <#else>
    <#local file = href>
    <#local anchor = ''>
  </#if>
  <@pp.add
      seq=pp.s.hrefs
      value={"source":pp.sourceFile, "href":href, "file":pp.outputRootRelativePath(file), "anchor":anchor}
  />
</#macro>

<#macro reportBugs>
  <div class="report-bugs">
    <p class="strong"><strong>Please report bugs you find!</strong> Any programming, documentation content or grammatical mistakes (even minor typos). Thank you!</p>
    <p>Use the <a href="http://sourceforge.net/tracker/?func=add&amp;group_id=74591&amp;atid=541453">bug reporting Web page</a>,<br>
      or e-mail: <@myEmail /></p>

    <p>Please report FreeMarker bugs at the <a rel="nofollow" href="http://sourceforge.net/tracker/?func=add&amp;group_id=794&amp;atid=100794">FreeMarker bug reporting Web page</a>, not for me. If you are not sure if you have found a FreeMarker or FMPP bug, just report it as an FMPP bug.</p>
  </div>

  <#assign P_reportBugPrinted = true>
</#macro>

<#macro myEmail plainText=false>
  <#if plainText>
    ddekanyREMOVETHIS@freemail.hu (delete the "REMOVETHIS"!)<#t>
  <#else>
    <a href="mailto:ddekanyREMOVETHIS@freemail.hu">ddekanyREMOVETHIS@freemail.hu</a> (delete the "REMOVETHIS"!)<#t>
  </#if>
</#macro>

<#macro fmppPath path=''>
  <@c><@r>&lt;FMPP></@r><#if path != ''>/${path}</#if></@c><#t>
</#macro>

<#macro example path=''>
  <#if path != ''>
    <@fmppPath path="docs/examples/${path}" />
  <#else>
    <@fmppPath path="docs/examples" />
  </#if>
</#macro>

<#macro nbc><span class="warning"><strong>Warning!</strong> Incompatible change!</span> </#macro>

<#macro nbca><span class="warning"><strong>Warning!</strong> Incompatible Java API change!</span> </#macro>

<#macro attc><span class="warning"><strong>Attention!</strong> </span> </#macro>

<#macro url href rel=""><a href="${href}"<#if rel?has_content> rel="${rel}"</#if>>${href}</a></#macro>

<#macro fma href=''>
  <#if href = ''>
    <#local href='index.html'>
  </#if>
  <@a href="${pp.home}freemarker/${href}" rel="nofollow"><#nested></@a><#t>
</#macro>

</#escape>
</@pp.ignoreOutput>