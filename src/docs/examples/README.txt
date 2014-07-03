FMPP Examples
=============

How to try them
---------------

To run the examples, the command-line FMPP tool must be installed (see
the FMPP Manual). Then, go to the directory of the example, and simply
issue the fmpp command there. Or, you can run the example without
entering into its directory with: fmpp -C the/example/directory

The above does not apply for the Ant examples (their name starts with
"ant"). To run those, FMPP must be installed for Ant (see the FMPP
Manual). Then, go to the directory of the example, and simply issue the
ant command there. Or, you can run the example without
entering into its directory with: ant -f the/example/directory/build.xml

The output will be created in the out subdirectory of the example.

The build.xml in this directory: if you run it with Ant, it will run all
examples. In this case, the command-line FMPP tool need not be
installed.


List of examples
----------------

qtour_step...  The examples used in the Manual/Quick Tour chapter.
          1    - The idea of bulk processing
          2    - Data loading basics. Interpolations.
          3    - Data loading from multiple sources. Directives.
          4    - Multiple output files from single source file

capture        Generates Table of Contents on the top of the page
               based on the headers used further down, using
               block assignments to resolve the x-dependency.
               
border         Uses headers and footers to automate including,
               interpolation escaping, and to convert *.c files
               to HTML files.
               
session        Demonstrates session variables and turns with the
               generation of an index page that contains links to
               the other generated pages.
              
img_dims       Shows HTML <img> with calculated image dimensions.
               
csv            Detects the columns of a CSV file.
               Uses typed columns (numerical colum, boolean column).

check_links    Uses a macro instead of HTML <a href=...> that sends
               warning if the link is broken.
               
tdd            Loads data model from a tdd file.

xml            XML data loader basics.
               If you use earlier J2SE version than 1.4, the you have
               to install some JAXP 1.2+ implementation. Say, Crimson.

xml2           Product catalog that uses two related XML-s as data source.
               FreeMarker requires XPath support for this example; see the
               FMPP Manual/Installing for more information about XPath
               availability.

xml_try        You can play with xml data loader options here, and examine
               XML wrapping in general: It displays the resulting node
               tree.

xml_validating Demonstrates the usage of OASIS catalogs (public ID
               resolution), validation, and the usage of XML name spaces
               through the processing of an XHTML document.

xml_rendering  Renders XML source files to HTML-s, using "renderXml"
               processing mode. Also demonstrates features as indexing,
               declarative XML processing, XPath expressions.
               
eval           Demonstrates eval (BeanShell) data loader.

inherit_config Demonstrates the usage of configuration inheritance.

local_data     Demonstrates the usage of the localData setting.

ant            Uses FMPP as Ant task: Basics.
               You can't use the command-line tool here.

ant2           Uses FMPP as Ant task: More property data loaders.
               You can't use the command-line tool here.
               
ant3           Uses FMPP as Ant task: Executes qtour_step4 with
               changed text color.
               
multipage_list Lists a sequence on multiple pages, like a Google search
               result. Demonstrates the usage of BeanShell calculated local
               data to prevent template overcomplication. Kinf of MVC style
               separation.