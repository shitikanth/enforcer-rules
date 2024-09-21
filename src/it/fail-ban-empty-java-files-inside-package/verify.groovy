File file = new File( basedir, "build.log" )
assert file.exists()
String text = file.getText("utf-8");

assert text.contains('[ERROR] Rule 0: com.github.shitikanth.enforcerrules.BanEmptyJavaFiles failed with message:')
assert text.contains('[ERROR] Empty Java source files found:')
assert text.contains('\t- src/main/java/com/github/shitikanth/inside/Example.java')