File file = new File( basedir, "build.log" )
assert file.exists()
String text = file.getText("utf-8");

assert text.contains('[ERROR] Rule 0: io.github.shitikanth.enforcerrules.BanEmptyJavaFiles failed with message:')
assert text.contains('[ERROR] Java files with incorrect package declaration:')
assert text.contains('\t- src/main/java/com/example/Foo.java (expected: com.example, found: com.wrong)')
