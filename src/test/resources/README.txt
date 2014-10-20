FMPP test suite
---------------

Directories:

- tests: FMPP projects (test cases)
- expected: The expected output of the FMPP projects

The test suite can be run from the FMPP home directory with:

  ant test

To run only a single test case:

  ant test -Dfmpp.testcase=<testCaseName>

where <testCaseName> is the name of the subdirectory in the
"tests" directory.
