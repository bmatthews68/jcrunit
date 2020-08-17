JCR Unit
========

**JCR Unit** provides a [JUnit rule](https://github.com/junit-team/junit/wiki/Rules) for use when testing code that
depends on a [Java Content Repository 2.0](https://jcp.org/en/jsr/detail?id=283) implementation. It creates an in-memory
content repository using the [Jackrabbit Oak](https://jackrabbit.apache.org/oak/) implementation.

The current release only supports file (nt:file) and folder (nt:folder) node types.

Maven Central Coordinates
-------------------------
**LDAPUnit** has been published in [Maven Central](http://search.maven.org) at the following
coordinates:

```xml
<dependency>
    <groupId>com.btmatthews.jcrunit</groupId>
    <artifactId>jcrunit</artifactId>
    <version>2.0.0</version>
</dependency>
```

Credits
-------
Internally **JCRUnit** is using the [Jackrabbit Oak](https://jackrabbit.apache.org/oak/) to run the in-memory content
repository.

License & Source Code
---------------------
The **JCRUnit** is made available under the
[Apache License](http://www.apache.org/licenses/LICENSE-2.0.html) and the source code is hosted on
[GitHub](http://github.com) at https://github.com/bmatthews68/jcrunit.