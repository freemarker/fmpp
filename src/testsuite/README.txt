FMPP test suite
---------------

FMPP test suite is used to ensure that you don't break anything
when you modify/refactor FMPP source code.

Directoryes:

- java: Java classes used for the test
- projects: FMPP projects (test cases)
- references: The expected output of the projects

The test suite can be run from the FMPP home directory with:

  ant test

To run only a single test case:

  ant test -Dfmpp.testcase=<testCaseName>

where <testCaseName> is the name of the subdirectory in the
"projects" directory.


$Id: README.txt,v 1.6 2004/03/01 01:02:29 ddekany Exp $