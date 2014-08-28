lazy val root = (project in file(".")).enablePlugins(SbtWeb)

//JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

libraryDependencies += "org.webjars" % "bootstrap" % "3.0.2"

Less4jKeys.compress in Assets := false

Less4jKeys.compress in TestAssets := false

//includeFilter in (Assets, Less4jKeys.less4j) := "mapissues.less" | "bar.less"

//includeFilter in (Assets, LessKeys.less) := "foo.less" | "bar.less"
