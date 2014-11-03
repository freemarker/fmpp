<#ftl encoding="utf-8">
<@pp.dropOutputFile />

<@assert synthetic?size == 5 />
<@assert !synthetic?parent?? />
<@assert synthetic?node_name == "unnamedObject" />
<@assert synthetic?node_type == "object" />
<@assert !synthetic?node_namespace?? />

<#assign aString = synthetic.aString>
<@assert aString == "árvíztűrő" />
<@assert aString?is_string />
<@assert !aString?is_hash />
<@assert aString?parent?size == 5 />
<@assert aString?node_name == "aString" />
<@assert aString?node_type == "string" />

<#assign aNumber = synthetic.aNumber>
<@assert aNumber == 123 />
<@assert aNumber?is_number />
<@assert !aNumber?is_string />
<@assert !aNumber?is_hash />
<@assert aNumber?parent?size == 5 />
<@assert aNumber?node_name == "aNumber" />
<@assert aNumber?node_type == "number" />

<#assign aBoolean = synthetic.aBoolean>
<@assert aBoolean />
<@assert aBoolean?is_boolean />
<@assert !aBoolean?is_string />
<@assert !aBoolean?is_hash />
<@assert aBoolean?parent?size == 5 />
<@assert aBoolean?node_name == "aBoolean" />
<@assert aBoolean?node_type == "boolean" />

<#assign arr = synthetic.anArray>
<@assert arr?size == 7 />
<@assert arr[0] == 1.5 />
<@assert arr[0]?node_name == "unnamedNumber" />
<@assert arr[1] == "foo" />
<@assert arr[1]?node_name == "unnamedString" />
<@assert !arr[2]?? />
<@assert arr[3] />
<@assert arr[3]?node_name == "unnamedBoolean" />
<@assert !arr[4] />
<@assert arr[5]?size == 2 />
<@assert arr[5][0] == 11 />
<@assert arr[5][1] == 22 />
<@assert arr[5][1] == 22 />
<@assert arr[5][0] == 11 />
<@assert arr[5]?join(", ") == "11, 22" />
<@assert arr[5]?node_name == "unnamedArray" />
<@assert arr[6]?size == 2 />
<@assert arr[6]?node_name == "unnamedObject" />
<@assert arr[6].a == 111 />
<@assert arr[6].a?node_name == "a" />
<@assert arr[6].b == 222 />
<@assert arr[6]?parent[0] == 1.5 />
<@assert arr[6].b?parent?parent[0] == 1.5 />
<@assert arr[6].b?root.aNumber == 123 />
<@assert arr[6].b?ancestors('anArray')[0][0] == 1.5 />
<@assert !arr[7]?? />
<#list arr as e>
  <#if e_index == 1>
    <@assert e == "foo" />
  </#if>
</#list>
<@assert arr?is_sequence />
<@assert !arr?is_hash />
<@assert !arr?is_string />
<@assert arr?parent?size == 5 />
<@assert arr?node_name == "anArray" />
<@assert arr?node_type == "array" />

<#assign childTypes = "">
<#list arr?children as child>
  <#assign childTypes = childTypes + child?node_type + ";">
</#list>
<@assert childTypes == "number;string;null;boolean;boolean;array;object;", childTypes />

<#assign values = "">
<#list arr as value>
  <#assign values = values + ((value?node_type)!'?') + ";">
</#list>
<@assert values == "number;string;?;boolean;boolean;array;object;", values />

<#assign anObject = synthetic.anObject>
<@assert anObject?size == 4 />
<@assert anObject?is_hash />
<@assert !anObject?is_sequence />
<@assert !anObject?is_string />
<@assert anObject.u == 'U' />
<@assert anObject.n!'null' == 'null' />
<#list anObject.a as e>
  <#if e_index == 0>
    <@assert e == 1111 />
  <#elseif e_index == 1>
    <@assert e == 2222 />
  <#else>
    <@assert false />
  </#if>
</#list>
<@assert anObject.a?size == 2 />
<@assert anObject.a[1] == 2222 />
<@assert anObject.a[0] == 1111 />
<@assert anObject.a[0] == 1111 />
<@assert anObject.a[1] == 2222 />
<@assert anObject.a[1]?parent[0] == 1111 />
<@assert anObject.o?size == 2 />
<@assert anObject.o.b == 22222 />
<@assert anObject.o.a == 11111 />
<@assert anObject.o.a == 11111 />
<@assert anObject.o.b == 22222 />
<@assert anObject.o.b?parent.a == 11111 />
<@assert !anObject.n?? />
<@assert !anObject.wrong?? />
<@assert anObject?parent.aNumber == 123 />
<@assert anObject?node_name == "anObject" />
<@assert anObject?node_type == "object" />

<#assign childNames = "">
<#list anObject?children as child>
  <#assign childNames = childNames + child?node_name + ";">
</#list>
<@assert childNames == "u;n;a;o;", childNames />

<#assign values = "">
<#list anObject?values as value>
  <#assign values = values + ((value?node_name)!'?') + ";">
</#list>
<@assert values == "u;?;a;o;", values />
<@assert anObject?values[2]?node_type == 'array' />
<@assert anObject?values[2]?parent.u == 'U' />

<@assert anObject?keys?join(";", "?") == "u;n;a;o" />
<#assign keys = "">
<#list anObject?keys as key>
  <#assign keys = keys + ((key?node_name)!'?') + ";">
</#list>
<@assert keys == "unnamedString;unnamedString;unnamedString;unnamedString;", keys />
<@assert anObject?keys[2]?node_type == 'string' />
<@assert anObject?keys[2]?parent.u == 'U' />

<#macro assert bool actual="">
  <#if !bool>
    <#stop "Assertion failed" + (actual != "")?string(". Actual: " + actual, "")>
  </#if>
</#macro>
