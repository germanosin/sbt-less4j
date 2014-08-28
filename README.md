# Sbt-web Less4j Plugin 

This plugin based on [Less4j](https://github.com/SomMeri/less4j) for compiling less files

build.sbt config
```
Less4jKeys.compress in Assets := true // compress css files
Less4jKeys.sourceMap in Assets := true // compile sourcemap file
Less4jKeys.sourceMapFileInline in Assets := true // Whether the source map should be embedded in the output file
Less4jKeys.sourceMapLessInline in Assets := true // Whether to embed the less code in the source map

includeFilter in (Assets, Less4jKeys.less4j) := "main.less" | "foo.less" | "bar.less"
```


## Installation

Add a dependency to the plugins.sbt file:

```scala
addSbtPlugin("com.github.germanosin.sbt" % "sbt-less4j" % "1.0.0")
```

## Author

German Osin