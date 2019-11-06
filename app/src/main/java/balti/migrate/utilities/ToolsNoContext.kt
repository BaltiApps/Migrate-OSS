package balti.migrate.utilities

class ToolsNoContext {

    companion object{

        fun applyNamingCorrectionForShell(name: String) =
                name.replace("(", "\\(").replace(")", "\\)").replace(" ", "\\ ")


    }

}