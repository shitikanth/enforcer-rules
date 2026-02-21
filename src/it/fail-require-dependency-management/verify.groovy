File file = new File( basedir, "build.log" )
assert file.exists()
String text = file.getText("utf-8")

assert text.contains('[ERROR] Rule 0: io.github.shitikanth.enforcerrules.RequireDependencyManagement failed with message:')
assert text.contains('[ERROR] Dependencies without dependency management:')
assert text.contains('\t- org.junit.jupiter:junit-jupiter:5.11.0')
