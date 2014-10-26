<@pp.ignoreOutput>

<#--
"page" macro and its private helpers.
-->

<#escape x as x?html>

<#macro page title keywords toc=true>
  <#assign P_sectLevel = 0>
  <#assign P_sectId = 0>
  <#assign P_sects = pp.newWritableSequence()>
  <#assign P_index = pp.newWritableSequence()>
  <#assign P_settings_in_context = pp.newWritableSequence()>

  <#local navCtx = page_buildNavigationContext(title)>

  <@pp.restartOutputFile />
  <#compress>
  <!doctype html>
  <!--[if lte IE 8]><html class="ie8" lang="en"> <![endif]-->
  <!--[if gt IE 8]><!--><html lang="en"> <!--<![endif]-->
  <head prefix="og: http://ogp.me/ns#">
    <meta http-equiv="Content-Type" content="text/html; charset=${pp.outputEncoding}">
    <meta http-equiv="Content-Script-Type" content="text/javascript">
    <meta name="Keywords" content="FMPP, preprocessor, FreeMarker, template, templates, HTML, HTML template, HTML templates, text, macro, macros, text preprocessor, HTML preprocessor, static HTML, HTML generator, static HTML generator, Java, free, open source, open-source, ${keywords}">
    <#if !navCtx.isTheIndexPage>
      <title>${title} - FMPP</title>
      <meta property="og:title" content="${title} - FMPP">
    <#else>
      <meta property="og:description" content="Command-line/Ant-task/embeddable text file preprocessor. Macros, flow control, expressions. Recursive directory processing. Extendable in Java to display data from any data sources (as database). Can generate complete homepages (tree of HTML-s, images).">
      <meta name="Description" content="Command-line/Ant-task/embeddable text file preprocessor. Macros, flow control, expressions. Recursive directory processing. Extendable in Java to display data from any data sources (as database). Can generate complete homepages (tree of HTML-s, images).">
      <title>FMPP: Text file preprocessor (HTML preprocessor)</title>
      <meta property="og:title" content="FMPP: Text file preprocessor (HTML preprocessor)">
    </#if>
    <meta property="og:locale" content="en_US">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="format-detection" content="telephone=no">

    <#if online>
      <link rel="stylesheet" type="text/css" href="${pp.home}style/main.min.css" />
    <#else>
      <link rel="stylesheet" type="text/css" href="${pp.home}style/main.css" />
    </#if>
  </head>

  <body itemscope itemtype="http://schema.org/Article">

    <@page_header navCtx=navCtx title=title />

    <div class="page-wrapper">
      <div class="site-width">
  </#compress>
        <#if !navCtx.isTheIndexPage>
          <h1 itemprop="name">${title}</h1>
        </#if>

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
  <#compress>
      </div>
    </div>

    <div class="site-footer">
      <div class="site-width footer-inner">
        <div class="footer-left">
          <@page_pagers navCtx />
        </div>

        <div class="footer-right">
          <div class="generated">
            Generated on <time itemprop="dateModified" datetime="${pp.now?iso_utc}" title="${pp.now?string.long}">${pp.now?string("MMM d, yyyy hh:mm a zzz")}</time><br>
            For FMPP version ${pp.version}
          </div>

          <div class="external-links">
            <a href="http://sourceforge.net" rel="nofollow">
              <#if online>
                <img src="http://sourceforge.net/sflogo.php?group_id=74591&amp;type=1" alt="SourceForge Logo">
              <#else>
                <@html.img src="${pp.home}style/sflogo.png" alt="SourceForge Logo"/>
              </#if>
            </a>
            <a href="http://freemarker.org"><@html.img src="${pp.home}style/poweredby_sq_simple.png" alt="Powered by FreeMarker" /></a><#t>
          </div>
        </div>
      </div>
    </div>
  </body>
  </html>
  </#compress>
  <#assign P_reportBugPrinted = false>
</#macro>


<#---
  @param navCtx
  @param title
-->
<#macro page_header navCtx title>
  <#compress>
    <div class="site-header">
      <div class="site-width ${navCtx.isTheIndexPage?string('home', 'inner')}-site-header">
        <#if navCtx.isTheIndexPage>
          <div class="home-header">
            <div class="logo">
              <img src="${pp.home}style/fmpptitle.png" alt="FMPP" />
            </div>
            <span itemprop="name">${title}</span>
            <br>Version ${pp.version}
          </div>
        <#else>
          <div class="header-left">
            <a href="${pp.home}index.html" class="logo" role="banner">
              <img src="${pp.home}style/fmpptitle.png" alt="FMPP" />
            </a>
            <@page_breadcrumbs navCtx />
          </div>
          <div class="header-right">
            <@page_pagers navCtx, false />
          </div>
        </#if>
      </div>
    </div>
  </#compress>
</#macro>


<#macro page_pagers navCtx bugReportingIcon=true>
  <nav>
    <div class="pagers">
      <#if navCtx.showPagerButtons>
        <#if navCtx.prevLink != "">
          <a href="${navCtx.prevLink}"><@html.img src='${pp.home}style/prev.png' alt='Prev' /></a><#t>
        <#else>
          <@html.img src='${pp.home}style/prev_gray.png' alt='-' /><#t>
        </#if>
        <#if navCtx.nextLink != "">
          <a href="${navCtx.nextLink}"><@html.img src='${pp.home}style/next.png' alt='Next' /></a><#t>
        <#else>
          <@html.img src='${pp.home}style/next_gray.png' alt='-' /><#t>
        </#if>
        <#if !navCtx.isContentsPage && navCtx.tocLink != "">
          <a href="${navCtx.tocLink}"><@html.img src='${pp.home}style/contents.png' alt='Contents' /></a><#t>
        <#else>
          <@html.img src='${pp.home}style/contents_gray.png' alt='-' /><#t>
        </#if>
      </#if>
      <#if !navCtx.isTheIndexPage>
        <@a href="${pp.home}index.html"><@html.img src='${pp.home}style/home.png' alt='Home' /></@a>
      </#if>
      <#if bugReportingIcon && !P_reportBugPrinted?default(false)>
          <@a href="${pp.home}reportbug.html"><@html.img src='${pp.home}style/reportbug.png' alt='Report bug' /></@a>
      </#if>
    </div>
  </nav>
</#macro>

<#macro page_breadcrumbs navCtx>
  <#if navCtx.page_breadcrumbs?size == 0>
    <#stop "Couldn't find page_breadcrumbs path to: " + pp.outputFileName>
  </#if>
  <nav><#t>
    <ul class="breadcrumbs"><#t>
      <li itemscope itemtype="http://data-vocabulary.org/Breadcrumb"><#t>
        <a href="${pp.home}index.html" itemprop="url"><span itemprop="title">Home</span></a><#t>
      </li><#t>
      <#list navCtx.page_breadcrumbs as step>
        <li itemscope itemtype="http://data-vocabulary.org/Breadcrumb"><#t>
          <span class="icon icon-arrow-right2"></span><#t>
          <a href="${pp.home + step.file}" itemprop="url"><span itemprop="title">${step.title}</span></a><#t>
        </li><#t>
      </#list>
    </ul><#t>
  </nav><#t>
</#macro>

<#function page_getBreadcrumbsTo targetFile cur=manualFiles parentPath=[]>
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
      <#local path = page_getBreadcrumbsTo(targetFile, child, parentPath + [ cur[0] ])>
      <#if path?size != 0>
        <#-- Found it: -->
        <#return path>
      </#if>
  </#list>
  <#-- Not found under cur: -->
  <#return []>
</#function>

<#function page_buildNavigationContext title>
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

  <#return {
      'isTheIndexPage': pp.outputFileName == "index.html",
      'isContentsPage': pp.outputFileName == "manual.html",
      'showPagerButtons': showPagerButtons,
      'tocLink': tocLink,
      'prevLink': prevLink,
      'nextLink': nextLink,
      'page_breadcrumbs': page_getBreadcrumbsTo(pp.outputFileName)
  }>
</#function>

</#escape>
</@pp.ignoreOutput>