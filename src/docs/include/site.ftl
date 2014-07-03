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
  <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
  
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=${pp.outputEncoding}">
    <meta http-equiv="Content-Script-Type" content="text/javascript">
    <meta name="Keywords" content="FMPP, preprocessor, FreeMarker, template, templates, HTML, HTML template, HTML templates, text, macro, macros, text preprocessor, HTML preprocessor, static HTML, HTML generator, static HTML generator, Java, free, open source, open-source, ${keywords}">
    <#if !isTheIndexPage>
      <title>FMPP - ${title}</title>
    <#else>
      <meta name="Description" content="Command-line/Ant-task/embeddable text file preprocessor. Macros, flow control, expressions. Recursive directory processing. Extendable in Java to display data from any data sources (as database). Can generate complete homepages (tree of HTML-s, images).">
      <title>FMPP: Text file preprocessor (HTML preprocessor)</title>
    </#if>
    <style type="text/css">
      P {font-family: ${P_font}}
      UL {font-family: ${P_font}}
      LI {font-family: ${P_font}}
      TH {font-family: ${P_font}}
      TD {font-family: ${P_font}}
      H1 {font-family: ${P_font}}
      H2 {font-family: ${P_font}}
      H3 {font-family: ${P_font}}
      H4 {font-family: ${P_font}}
      A.toc{color: #0000CC; text-decoration:none}
      A.toc:hover{text-decoration: underline}
      A.toc:visited{color: #800080}
      A.s{color: #30D000}
      A.s:visited{color: #30D000}
      TT {font-size: 1em; font-family: "Courier New", monospace}
      CODE {font-size: 1em; font-family: "Courier New", monospace}
    </style>
  </head>

  <body background="img/background.png" bgcolor="#FFFFFF" text="#000000" style="font-family: ${P_font}">
    <table border=0 cellspacing=0 cellpadding=0 width="100%">
      <tr>
	<#if isTheIndexPage>
          <td valign="middle" align="center">
            <img src="img/fmpptitle.png" alt="FMPP">
            <br>FreeMarker-based text file PreProcessor
	    <br>Version ${pp.version}<br>&nbsp;	
	<#else>
          <td valign="middle" align="left">
	    <h1 style="font-family: ${P_font}"><font color="${P_titleColor}">${title}</font></h1>
	</#if>
        <td valign="middle" align="right">&nbsp;&nbsp;
        <td valign="middle" align="right">
        &nbsp;<br><#t>
        <#if showPagerButtons>
          &nbsp;&nbsp;<#t>
          <#if prevLink != "">
            <a href="${prevLink}"><@img 'prev.png', 'Prev' /></a><#t>
          <#else>
            <@img 'prev_gray.png', '-' /><#t>
          </#if>
          &nbsp;&nbsp;<#t>
          <#if nextLink != "">
            <a href="${nextLink}"><@img 'next.png', 'Next' /></a><#t>
          <#else>
            <@img 'next_gray.png', '-' /><#t>
          </#if>
          &nbsp;&nbsp;<#t>
          <#if !isContentsPage && tocLink != "">
            <a href="${tocLink}"><@img 'contents.png', 'Contents' /></a><#t>
          <#else>
            <@img 'contents_gray.png', '-' /><#t>
          </#if>
        </#if>
        <#if !isTheIndexPage>
          &nbsp;&nbsp;<a href="${pp.home}index.html"><@img 'home.png', 'Home' /></a><#t>
        </#if>
        <br>&nbsp;<#t>
        <@_ht_rows colspan=3/>
    </table>

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
        <p><b>Alphabetical index of keys:</b></p>
        <ul>
        <#list P_index?sort_by("title") as e>
          <li><a href="#${e.id}" class=toc>${e.title}</a>
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
    
    <@hr/>
    <table border=0 cellspacing=4 cellpadding=0 width="100%">
      <tr>
        <td valign="top" align="right">
          <table border=0 cellspacing=0 cellpadding=0 width="100%">
            <tr>
              <td align="left" valign="top"><#rt>
                <#if showPagerButtons>
                  <#if prevLink != "">
                    <a href="${prevLink}"><@img 'prev.png', 'Prev' /></a><#t>
                  <#else>
                    <@img 'prev_gray.png', '-' /><#t>
                  </#if>
                  &nbsp;&nbsp;<#t>
                  <#if nextLink != "">
                    <a href="${nextLink}"><@img 'next.png', 'Next' /></a><#t>
                  <#else>
                    <@img 'next_gray.png', '-' /><#t>
                  </#if>
                  &nbsp;&nbsp;<#t>
                  <#if !isContentsPage && tocLink != "">
                    <a href="${tocLink}"><@img 'contents.png', 'Contents' /></a><#t>
                  <#else>
                    <@img 'contents_gray.png', '-' /><#t>
                  </#if>
                  &nbsp;&nbsp;<#t>
                </#if>
                <#if !isTheIndexPage>
                  <@a href="${pp.home}index.html"><@img 'home.png', 'Home' /></@a>&nbsp;&nbsp;<#t>
                  &nbsp;&nbsp;&nbsp;&nbsp;<#t>
                </#if>
                <#if !P_reportBugPrinted?default(false)>
                    <@a href="${pp.home}reportbug.html"><@img 'reportbug.png', 'Report bug' /></@a>&nbsp;&nbsp;<#t>
                </#if>
              </td><#lt>
              <td align="right" valign="top">
                <i>
                  Generated&nbsp;on&nbsp;${pp.now?string("MMM d, yyyy hh:mm a zzz")}<br>
                  For&nbsp;FMPP&nbsp;version&nbsp;${pp.version}
                </i>
              </td>
            </tr>
          </table>
        </td>
      <tr>
        <td valign="middle" align="right"><#t>
          <#if online && isTheIndexPage>
            <a href="http://sourceforge.net"><img src="http://sourceforge.net/sflogo.php?group_id=74591&amp;type=1" border="0" alt="SourceForge Logo"></a><#t>
          <#else>
            <a href="http://sourceforge.net"><@img "sflogo.png", "SourceForge Logo"/></a><#t>
          </#if>
          &nbsp;&nbsp;<#t>
          <a href="http://freemarker.org"><@img "poweredby_sq_simple.png", "Powered by FreeMarker" /></a><#t>
        </td><#t>
      </tr>
    </table>
  </body>
  </html>
  <#assign P_reportBugPrinted = false>
</#macro>

<#macro img src alt>
  <#local label><#nested></#local>
  <#if label?length != 0>
    <p align=center>
  </#if>
  <@html.img src="${pp.home}img/${src}" alt=alt border=0/><#t>
  <#if label?length != 0>
    <br><#lt>
    <br><b>Figure:</b> <i><#noescape>${label}</#noescape></i>
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
     <h2><font color="${P_titleColor}"><#rt>
     <#if title?starts_with('ex:')>
       <@title[3..(title?length-1)]?interpret /><#t>
     <#else>
       ${title}<#t>
     </#if>
     </font></h2><#lt>
     <#nested>
     <@pp.clear seq=P_settings_in_context />
  <#elseif P_sectLevel = 2>
     <@.namespace.anchor anchor />
     <h3><font color="${P_titleColor}"><#rt>
     <#if title?starts_with('ex:')>
       <@title[3..(title?length-1)]?interpret /><#t>
     <#else>
       ${title}<#t>
     </#if>
     </font></h3><#lt>
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
    <p>
    <table border=0 cellspacing=0 cellpadding=0 width="100%">
      <@_ht_rows/>
    </table>
</#macro>

<#macro _ht_rows colspan=1>
  <#if colspan != 1>
    <#local cs = "colspan=" + colspan>
  <#else>
    <#local cs = "">
  </#if>
  <tr><td ${cs} height=1 bgcolor="#000000"><img src="img/none.gif" width=1 height=1 alt=""></td></tr>
  <tr><td ${cs} height=1 bgcolor="#808080"><img src="img/none.gif" width=1 height=1 alt=""></td></tr>
  <tr><td ${cs} height=1 bgcolor="#C0C0C0"><img src="img/none.gif" width=1 height=1 alt=""></td></tr>
  <tr><td ${cs} height=1 bgcolor="#E0E0E0"><img src="img/none.gif" width=1 height=1 alt=""></td></tr>
  <tr><td ${cs} height=1 bgcolor="#F0F0F0"><img src="img/none.gif" width=1 height=1 alt=""></td></tr>
</#macro>

<#macro c><code><font color="#017D01"><#nested></font></code></#macro>

<#macro r><i><#nested></i></#macro>

<#macro e><b><#nested></b></#macro>

<#macro note><p><i><b>Note:</b> <#nested></i></p></#macro>

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
  <code><#if link><a href="${pp.pathTo('/settings.html')}#key_${name}" class=s></#if><#t>
  <font color="#20C000">${name}</font><#t>
  <#if link></a></#if></code><#t>
</#macro>

<#macro prg escape=true>
<div align="left"><#t>
  <table bgcolor="#FFFFFF" cellspacing="0" cellpadding="0" border="0"><#t>
   <tr valign="top"><#t>
     <td height="1" width="1" bgcolor="black"><#t>
       <img src="img/none.gif" width="1" height="1" alt="" hspace="0" vspace="0" border="0"><#t>
     </td><#t>
     <td height="1" bgcolor="black"><#t>
       <img src="img/none.gif" width="1" height="1" alt="" hspace="0" vspace="0" border="0"><#t>
     </td><#t>
     <td height="1" width="1" bgcolor="black"><#t>
       <img src="img/none.gif" width="1" height="1" alt="" hspace="0" vspace="0" border="0"><#t>
     </td><#t>
   </tr><#t>
   <tr><#t>
     <td width="1" bgcolor="black"><#t>
       <img src="img/none.gif" width="1" height="1" alt="" hspace="0" vspace="0" border="0"><#t>
     </td><#t>
     <td><#t>
       <table bgcolor="#FFFFFF" cellspacing="0" cellpadding="4" border="0" width="100%" style="margin: 0px"><#t>
         <tr><#t>
           <td><#t>
             <pre style="margin: 0px"><#t>
               <#local body><#nested></#local>
               <#if !escape>
                 <#noescape>${body?chop_linebreak}</#noescape><#t>
               <#else>
                 ${body?chop_linebreak}<#t>
               </#if>
               <span style="font-size: 1pt"> </span><#t>
             </pre><#t>
           </td><#t>
         </tr><#t>
       </table><#t>
     </td><#t>
     <td width="1" bgcolor="black"><#t>
       <img src="img/none.gif" width="1" height="1" alt="" hspace="0" vspace="0" border="0"><#t>
     </td><#t>
   </tr><#t>
   <tr valign="top"><#t>
     <td height="1" width="1" bgcolor="black"><#t>
       <img src="img/none.gif" width="1" height="1" alt="" hspace="0" vspace="0" border="0"><#t>
     </td><#t>
     <td height="1" bgcolor="black"><#t>
       <img src="img/none.gif" width="1" height="1" alt="" hspace="0" vspace="0" border="0"><#t>
     </td><#t>
     <td height="1" width="1" bgcolor="black"><#t>
       <img src="img/none.gif" width="1" height="1" alt="" hspace="0" vspace="0" border="0"><#t>
     </td><#t>
   </tr><#t>
  </table><#t>
</div><#t>
</#macro>

<#macro toc title=''>
  <#if title != ''>
    <p><font color="${P_titleColor}"><b>${title}</b></font>
  </#if>
  <ul>
    <#nested>
  </ul>
</#macro>

<#macro toci href title>
  <#if href=''>
    <li>${title}<#nested></li>
  <#else>
    <li><a href="${href}" class=toc>${title}</a><#nested></li>
  </#if>
</#macro>

<#macro setting name type default merging clShort='' deprecated='' antAltAtt=''>
  <#if !stdSettings[name]??>
    <#stop 'No such standard setting exists: ${name}'>
  </#if>
  <@_index name />
  <@pp.add seq=P_settings_in_context value=name />
  <p>
    <i>Name:</i> <b>${name}</b><br>
    <#if deprecated?length != 0>
      <i><font color=red>Deprecated!</font></i> <@deprecated?interpret /><br>
    </#if>
    <i>Type:</i> ${type}<br>
    <i>Default:</i> <#rt>
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
      <i>Command-line short name: </i><tt>${clShort}</tt><br><#lt>
    </#if>
    <#if antAltAtt != ''>
      <i>Ant task attribute name alternative: </i><tt>${antAltAtt}</tt><br><#lt>
    </#if>
  </p>
</#macro>

<#macro variable name type nestedContent=false result='' anchor='' deprecated=''>
  <@_checkType type />
  <@_index name />
  <@sect title=name anchor=anchor>
    <#if deprecated != ''>
      <b><i>Deprecated:</i></b> <@deprecated?interpret /><br> 
    </#if>
    <i>Type:</i> ${type}
    <#assign P_params = pp.newWritableSequence()>
    <#local body><#nested></#local>
    <#if (type = 'directive' || type = 'method')>
        <#if type="directive">
          <br><i>Supports nested content:</i> ${nestedContent?string("yes", "no")}
        <#else>
          <br><i>Result type:</i> ${result}
        </#if>
    	<br><i>Parameters:</i>
    	<#if P_params?size != 0>
          <#if type="directive">
	        <ul>
	          <#list P_params as param>
              <li><b>${param.name}</b><#if param.default != ''>=${param.default}</#if>:
                ${param.type}.
	            <#noescape>${param.desc}</#noescape>
	          </#list>
	        </ul>
	   	  <#else>
	        <ol>
	          <#list P_params as param>
	          <li><b>${param.name}</b>: ${param.type}<#if param.optional>, optional</#if>.
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
	<i>Parameters:</i>
	<#if P_params?size != 0>
      <ol>
        <#list P_params as param>
        <li><b>${param.name}</b>: ${param.type}<#if param.optional>, optional</#if>.
          <#noescape>${param.desc}</#noescape>
        </#list>
      </ol>
	<#else>
	  none<BR>
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
  <table border=0 cellspacing=0 cellpadding=4><tr>
    <td align="left" valign="middle"><@img "bug.png", "bug" /></td>
    <td align="left" valign="middle">
      <font color=red size="+1"><b>Please report bugs you find!</b> Any programming, documentation content or grammatical mistakes, even minor typos. Thank you!</font><br>
      Use the <a href="http://sourceforge.net/tracker/?func=add&amp;group_id=74591&amp;atid=541453">bug reporting Web page</a>,<br> 
      or e-mail: <@myEmail /><br>
    </td>
  </tr>
  <tr>
    <td></td>
    <td>
      Please report FreeMarker bugs at the <a href="http://sourceforge.net/tracker/?func=add&amp;group_id=794&amp;atid=100794">FreeMarker bug reporting Web page</a>, not for me.
      If you are not sure if you have found a FreeMarker or an FMPP bug, just report it as FMPP bug.      
    </td>
  </tr>
  <tr>
    <td></td>
    <td>
      Also, note the <@a href="knownproblems.html">Known Problems</@a> page.
    </td>
  </tr>
  </table>
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

<#macro uc>
  <table border=0 cellspacing=0 cellpadding=4>
    <tr>
      <td align="left" valign="middle"><@img "underconstrucion.gif", "Under construction"/>
      <td align="left" valign="middle"><b>The documentation is under construction...</b>
  </table>
</#macro>

<#macro nbc><font color=red><b>Warning!</b> Incompatible change!</font> </#macro>

<#macro nbca><font color=red><b>Warning!</b> Incompatible Java API change!</font> </#macro>

<#macro url href><a href="${href}">${href}</a></#macro>

<#macro fma href=''>
  <#if href = ''>
    <#local href='index.html'>
  </#if>
  <@a href="${pp.home}freemarker/${href}"><#nested></@a><#t>
</#macro>

</#escape>
</@pp.ignoreOutput>