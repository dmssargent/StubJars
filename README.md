# StubJars

Creates a set of Java source files containing the minimum amount of
code to create a JAR with an identical public signature

## Passing Tests
* Creates a compilable set of Java source files for GSON

## Blocking Tests
* Creates a compilable set of Java source files for javac
* Creates a compilable set of Java source files for rt.jar

## Blocking Issues
* Generics and constructors are not playing well

Sample Error:
```text
stub_src\javax\lang\model\util\SimpleAnnotationValueVisitor7.java:6: error: constructor SimpleAnnotationValueVisitor6 in class SimpleAnnotationValueVisitor6<R#2,P> cannot be applied to given types;
protected SimpleAnnotationValueVisitor7(R arg0) {
                                                ^
  required: R#1
  found: no arguments
  reason: actual and formal argument lists differ in length
  where R#1,R#2,P are type-variables:
    R#1 extends Object declared in class SimpleAnnotationValueVisitor7
    R#2 extends Object declared in class SimpleAnnotationValueVisitor6
    P extends Object declared in class SimpleAnnotationValueVisitor6
```

* Doesn't have a public API for passing in Class objects directly

## How to use
1. Download StubJars
    ```bash
    $ git clone https://github.com/dmssargent/StubJars
    ```
1. Run StubJars, specifying the JARs you want to convert
    ```bash
    $ ./gradlew run jarFileA.jar jarFileB.jar...
    ```
    
## Generated Source Directory
In the current directory after you run StubJars, you will find a folder
called stub_src. This folder is the result of the StubJars and it 
contains the Java source code stubs for the specified JAR files

### How to compile the generated source
It's simple, you can use the following one liner
```bash
    $ $JAVA_HOME/bin/javac -d build/classes @sources.list
```

## License
Copyright 2018 David Sargent

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.