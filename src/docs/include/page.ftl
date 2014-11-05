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
  <!--[if lte IE 9]><html class="ie89" lang="en"> <![endif]-->
  <!--[if gt IE 9]><!--><html lang="en"> <!--<![endif]-->
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
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="format-detection" content="telephone=no">

    <#-- @todo: move seo tags to separate macro -->
    <meta name="og:url" content="${fmppWebsite}${pp.outputFileName}">
    <link rel="canonical" href="${fmppWebsite}${pp.outputFileName}">

    <#if navCtx.prevLink?has_content>
      <link rel="prev" href="${fmppWebsite}${navCtx.prevLink}">
    </#if>
    <#if navCtx.nextLink?has_content>
      <link rel="next" href="${fmppWebsite}${navCtx.nextLink}">
    </#if>

    <!--[if lt IE 9]>
       <script>
          document.createElement('nav');
          <#-- uncomment if we use other html5 elements for ie8
          document.createElement('header');
          document.createElement('section');
          document.createElement('article');
          document.createElement('aside');
          document.createElement('footer');
          -->
       </script>
    <![endif]-->
    <link href="http://fonts.googleapis.com/css?family=Droid+Sans+Mono" rel="stylesheet" type="text/css">
    <link rel="stylesheet" type="text/css" href="${pp.home}style/${online?string('main.min.css', 'main.css')}">
  </head>

  <body itemscope itemtype="http://schema.org/Article">

    <@page_header navCtx=navCtx title=title />

    <div class="page-wrapper">
      <div class="site-width">
  </#compress>
        <h1 itemprop="name">${title}</h1>

        <#local needHr = false>
        <#if toc>
          <#local content>
            <#nested>
          </#local>
          <div class="content-inner">
            <#if (P_index?size != 0 || P_sects?size != 0 || needHr)>
              <div class="content-left">
                <#if P_sects?size != 0>
                  <@.namespace.toc title="Page contents">
                  <#list P_sects as sect>
                    <@toci href="#" + sect.id title=sect.title/>
                  </#list>
                  </@>
                  <#local needHr = true>
                </#if>
                <#if P_index?size != 0>
                  <p class="toc-header alphabetical-index">Alphabetical index of keys:</p>
                  <ul class="table-of-contents">
                    <#list P_index?sort_by("title") as e>
                      <li><a href="#${e.id}">${e.title}</a>
                    </#list>
                  </ul>
                  <#local needHr = true>
                </#if>
                <#if needHr>
                  <#--><@hr/>-->
                </#if>
              </div>
            </#if>
            <div class="page-content content-right">
              <#noescape>${content}</#noescape>
            </div>
          </div>
        <#else>
          <div class="page-content">
            <#nested>
          </div>
        </#if>
  <#compress>
      </div>
    </div>

    <div class="site-footer">
      <div class="site-width footer-inner">
        <div class="footer-left">
          <@page_pagers navCtx />
          <@page_social />
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
            <a href="${freemarkerWebsite}"><@html.img src="${pp.home}style/poweredby_sq_simple.png" alt="Powered by FreeMarker" /></a><#t>
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
            ${title}
            <br>Version ${pp.version}
            <@page_social />
          </div>
        <#else>
          <div class="header-left"><#t>
            <a href="${pp.home}index.html" class="logo" role="banner">
              <img src="${pp.home}style/fmpptitle.png" alt="FMPP" />
            </a>
            <@page_breadcrumbs navCtx />
          </div><#t>
          <div class="header-right"><#t>
            <@page_pagers navCtx, false />
          </div><#t>
        </#if>
      </div>
    </div>
  </#compress>
</#macro>


<#macro page_pagers navCtx bugReportingIcon=true>
  <#if navCtx.showPagerButtons || !navCtx.isTheIndexPage || (bugReportingIcon && !P_reportBugPrinted?default(false))>
    <nav>
      <ul class="pagers"><#t>
        <#if navCtx.showPagerButtons>
          <#if navCtx.prevLink != "">
            <li><#t>
              <a class="pager-icon previous" href="${navCtx.prevLink}">Prev</a><#t>
            </li><#t>
          <#else>
            <li class="pager-icon previous">Prev</li><#t>
          </#if>
          <#if navCtx.nextLink != "">
            <li><#t>
              <a class="pager-icon next" href="${navCtx.nextLink}">Next</a><#t>
            </li><#t>
          <#else>
            <li class="pager-icon next">Next</li><#t>
          </#if>
          <#if !navCtx.isContentsPage && navCtx.tocLink != "">
            <li><#t>
              <a class="pager-icon contents" href="${navCtx.tocLink}">Contents</a><#t>
            </li><#t>
          <#else>
            <li class="pager-icon contents">Contents</li><#t>
          </#if>
        </#if>
        <#if !navCtx.isTheIndexPage>
          <li><#t>
            <a class="pager-icon home" href="${pp.home}index.html">Home</a><#t>
          </li><#t>
        </#if>
        <#if bugReportingIcon && !P_reportBugPrinted?default(false)>
          <li><#t>
            <a class="pager-icon report-bug" href="${pp.home}reportbug.html">Report bug</a><#t>
          </li><#t>
        </#if>
      </ul>
    </nav>
  </#if>
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

<#macro page_social>
  <#local socialLinks = [
    {
      "url": "https://github.com/freemarker/fmpp",
      "class": "github",
      "title": "Github"
    }, {
      "url": "https://twitter.com/freemarker",
      "class": "twitter",
      "title": "Twitter"
    }
  ]>

  <ul class="social-icons"><#t>
      <#list socialLinks as link>
        <li><#t>
          <a class="${link.class}" href="${link.url}">${link.title}</a><#t>
        </li><#t>
      </#list>
    </ul><#t>

</#macro>

</#escape>
</@pp.ignoreOutput>