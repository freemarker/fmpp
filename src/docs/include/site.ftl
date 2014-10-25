<@pp.ignoreOutput>
<#escape x as x?html>

<#assign P_titleColor = "#000000">
<#assign P_font = "Arial, sans-serif">

<#macro page title keywords toc=true>
  <#assign P_sectLevel = 0>
  <#assign P_sectId = 0>
  <#assign P_sects = pp.newWritableSequence()>
  <#assign P_index = pp.newWritableSequence()>
  <#assign P_settings_in_context = pp.newWritableSequence()>

  <#local isTheIndexPage = (pp.outputFileName = "index.html")>
  <#local isContentsPage = (pp.outputFileName = "manual.html")>
  <#local tocLink = "">
  <#local prevLink = "">
  <#local nextLink = "">

  <#local showPagerButtons = false>
  <#list flattenedManualFiles as f>
    <#if pp.outputFile = f>
      <#local showPagerButtons = true>
      <#if !pp.s.manualFileTitles??>
        <@pp.set hash=pp.s key="manualFileTitles" value=pp.newWritableHash()/>
      </#if>
      <@pp.set hash=pp.s.manualFileTitles key=f value=title/>
      <#if f_index != 0>
        <#local prevLink = pp.home + flattenedManualFiles[f_index - 1]>
      </#if>
      <#if f_has_next>
        <#local nextLink = pp.home + flattenedManualFiles[f_index + 1]>
      </#if>
      <#local tocLink = pp.home + "manual.html">
      <#break>
    </#if>
  </#list>

  <@pp.restartOutputFile />
  <!doctype html>
  <html lang="en">
  <head prefix="og: http://ogp.me/ns#">
    <meta http-equiv="Content-Type" content="text/html; charset=${pp.outputEncoding}">
    <meta http-equiv="Content-Script-Type" content="text/javascript">
    <meta name="Keywords" content="FMPP, preprocessor, FreeMarker, template, templates, HTML, HTML template, HTML templates, text, macro, macros, text preprocessor, HTML preprocessor, static HTML, HTML generator, static HTML generator, Java, free, open source, open-source, ${keywords}">
    <#if !isTheIndexPage>
      <title>FMPP - ${title}</title>
      <meta property="og:title" content="FMPP - ${title}">
    <#else>
      <meta property="og:description" content="Command-line/Ant-task/embeddable text file preprocessor. Macros, flow control, expressions. Recursive directory processing. Extendable in Java to display data from any data sources (as database). Can generate complete homepages (tree of HTML-s, images).">
      <meta name="Description" content="Command-line/Ant-task/embeddable text file preprocessor. Macros, flow control, expressions. Recursive directory processing. Extendable in Java to display data from any data sources (as database). Can generate complete homepages (tree of HTML-s, images).">
      <title>FMPP: Text file preprocessor (HTML preprocessor)</title>
      <meta property="og:title" content="FMPP: Text file preprocessor (HTML preprocessor)">
    </#if>
    <meta property="og:locale" content="en_US">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="format-detection" content="telephone=no">
    
    <link rel="stylesheet" type="text/css" href="${pp.home}style/main.css" />
    <!--[if lt IE 9]>
      <link rel="stylesheet" type="text/css" href="${pp.home}style/main-ie8-or-less.css" />
    <![endif]-->
  </head>

  <body itemscope itemtype="http://schema.org/Article">
    <#if !isTheIndexPage>
      <div class="logo-banner"><#t>
        <div class="site-width"><#t>
          <@breadcrumbs />
        </div><#t>
      </div><#t>
    </#if>

    <div class="site-header">
      <div class="site-width">
        <div class="pagers">
          <#if showPagerButtons>
            <#if prevLink != "">
              <a href="${prevLink}"><@html.img src='${pp.home}style/prev.png' alt='Prev' /></a><#t>
            <#else>
              <@html.img src='${pp.home}style/prev_gray.png' alt='-' /><#t>
            </#if>
            <#t>
            <#if nextLink != "">
              <a href="${nextLink}"><@html.img src='${pp.home}style/next.png' alt='Next' /></a><#t>
            <#else>
              <@html.img src='${pp.home}style/next_gray.png' alt='-' /><#t>
            </#if>
            <#if !isContentsPage && tocLink != "">
              <a href="${tocLink}"><@html.img src='${pp.home}style/contents.png' alt='Contents' /></a><#t>
            <#else>
              <@html.img src='${pp.home}style/contents_gray.png' alt='-' /><#t>
            </#if>
          </#if>
          <#if !isTheIndexPage>
            <a href="${pp.home}index.html"><@html.img src='${pp.home}style/home.png' alt='Home' /></a><#t>
          </#if>
        </div>

        <#if isTheIndexPage>
          <div class="home-header">
            <img src="${pp.home}style/fmpptitle.png" alt="FMPP">
            <span itemprop="name">FreeMarker-based text file PreProcessor</span>
            <br>Version ${pp.version}
          </div>
        <#else>
          <h1 itemprop="name">${title}</h1>
        </#if>
      </div>
    </div>

    <div class="page-wrapper">
      <div class="site-width">
        <#local needHr = false>
        <#if toc>
          <#local content>
            <#nested>
          </#local>
          <#if P_sects?size != 0>
            <@.namespace.toc title="Page contents">
            <#list P_sects as sect>
              <@toci href="#" + sect.id title=sect.title/>
            </#list>
            </@>
            <#local needHr = true>
          </#if>
          <#if P_index?size != 0>
            <p class="toc-header">Alphabetical index of keys:</p>
            <ul class="table-of-contents">
            <#list P_index?sort_by("title") as e>
              <li><a href="#${e.id}">${e.title}</a>
            </#list>
            </ul>
            <#local needHr = true>
          </#if>
          <#if needHr>
            <@hr/>
          </#if>
          <#noescape>${content}</#noescape>
        <#else>
          <#nested>
        </#if>
      </div>
    </div>

    <div class="site-footer">
      <div class="site-width">
        <div class="pagers">
          <#if showPagerButtons>
            <#if prevLink != "">
              <a href="${prevLink}"><@html.img src='${pp.home}style/prev.png' alt='Prev' /></a><#t>
            <#else>
              <@html.img src='${pp.home}style/prev_gray.png' alt='-' /><#t>
            </#if>
            <#if nextLink != "">
              <a href="${nextLink}"><@html.img src='${pp.home}style/next.png' alt='Next' /></a><#t>
            <#else>
              <@html.img src='${pp.home}style/next_gray.png' alt='-' /><#t>
            </#if>
            <#if !isContentsPage && tocLink != "">
              <a href="${tocLink}"><@html.img src='${pp.home}style/contents.png' alt='Contents' /></a><#t>
            <#else>
              <@html.img src='${pp.home}style/contents_gray.png' alt='-' /><#t>
            </#if>
          </#if>
          <#if !isTheIndexPage>
            <@a href="${pp.home}index.html"><@html.img src='${pp.home}style/home.png' alt='Home' /></@a>
          </#if>
          <#if !P_reportBugPrinted?default(false)>
              <@a href="${pp.home}reportbug.html"><@html.img src='${pp.home}style/reportbug.png' alt='Report bug' /></@a>
          </#if>
        </div>

        <div class="generated">
          Generated on <time itemprop="dateModified" datetime="${pp.now?iso_utc}" title="${pp.now?string.long}">${pp.now?string("MMM d, yyyy hh:mm a zzz")}</time><br>
          For FMPP version ${pp.version}
        </div>

        <div class="external-links">
          <#if online && isTheIndexPage>
            <a href="http://sourceforge.net" rel="nofollow"><img src="http://sourceforge.net/sflogo.php?group_id=74591&amp;type=1" alt="SourceForge Logo"></a><#t>
          <#else>
            <a href="http://sourceforge.net" rel="nofollow"><@html.img src="${pp.home}style/sflogo.png" alt="SourceForge Logo"/></a><#t>
          </#if>
          <a href="http://freemarker.org"><@html.img src="${pp.home}style/poweredby_sq_simple.png" alt="Powered by FreeMarker" /></a><#t>
        </div>
      </div>
    </div>
  </body>
  </html>
  <#assign P_reportBugPrinted = false>
</#macro>

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

<#macro a href>
  <a href="${href}"><#nested></a><#t>
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
    <p class="strong"><strong>Please report bugs you find!</strong> Any programming, documentation content or grammatical mistakes, even minor typos. Thank you!</p>
    <p>Use the <a href="http://sourceforge.net/tracker/?func=add&amp;group_id=74591&amp;atid=541453">bug reporting Web page</a>,<br>
      or e-mail: <@myEmail /></p>

    <p>Please report FreeMarker bugs at the <a rel="nofollow" href="http://sourceforge.net/tracker/?func=add&amp;group_id=794&amp;atid=100794">FreeMarker bug reporting Web page</a>, not for me. If you are not sure if you have found a FreeMarker or an FMPP bug, just report it as FMPP bug.</p>
    <p>Also, note the <@a href="knownproblems.html">Known Problems</@a> page.</p>
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

<#macro url href><a href="${href}">${href}</a></#macro>

<#macro fma href=''>
  <#if href = ''>
    <#local href='index.html'>
  </#if>
  <@a href="${pp.home}freemarker/${href}"><#nested></@a><#t>
</#macro>

<#macro breadcrumbs>
  <#local path = getBreadcrumbsTo(pp.outputFileName)>
  <#if path?size == 0>
    <#stop "Couldn't find breadcrumbs path to: " + pp.outputFileName>
  </#if>
  <nav>
    <ul class="breadcrumbs" itemtype="http://data-vocabulary.org/Breadcrumb">
      <li itemscope itemtype="http://data-vocabulary.org/Breadcrumb">
        <a href="${pp.home}index.html" class="logo">
          <data itemprop="title" value="FMPP Home"><img src="${pp.home}style/fmpptitle.png" alt="FMPP Home"></data>
        </a>
      </li>
      <#list path as step>
        <li itemscope itemtype="http://data-vocabulary.org/Breadcrumb">
          >
          <a href="${pp.home + step.file}" itemprop="url"><span itemprop="title">${step.title}</span></a>
        </li>
      </#list>
    </ul>
  </nav>
</#macro>

<#function getBreadcrumbsTo targetFile cur=manualFiles parentPath=[]>
  <#if !cur?is_sequence> <#-- leaf node -->
    <#if cur == targetFile>
      <#return parentPath + [ { 'file': cur, 'title': pp.s.manualFileTitles[cur] } ]>
    <#else>
      <#-- Not found under cur: -->
      <#return []>
    </#if>
  <#elseif cur[0].file == targetFile> <#-- non-leaf node == target -->
    <#return parentPath + [ cur[0] ]>
  </#if>
  
  <#list cur[1..] as child>
      <#local path = getBreadcrumbsTo(targetFile, child, parentPath + [ cur[0] ])>
      <#if path?size != 0>
        <#-- Found it: -->
        <#return path>
      </#if>
  </#list>
  <#-- Not found under cur: -->
  <#return []>
</#function>

</#escape>
</@pp.ignoreOutput>