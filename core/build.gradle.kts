plugins { kotlin("jvm"); application }
kotlin { jvmToolchain(17) }
dependencies { implementation("com.squareup.okhttp3:okhttp:4.12.0"); testImplementation("junit:junit:4.13.2"); testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0") }
application { mainClass.set("de.edgespeak.core.SmokeKt") }
