def call(String name) {
	switch(name) {
		// Max
        case "RadeonProRenderMaxPluginManual":
            return "manual";
            break;
        case "RadeonProRenderMaxPlugin-WeeklyFull":
            return "weekly";
            break;
        // Blender 2.8
		case "RadeonProRenderBlender2.8PluginManual":
            return "manual";
            break;
        case "RadeonProRenderBlender2.8Plugin-WeeklyFull":
            return "weekly";
            break;
        // Maya
	    case "RadeonProRenderMayaPluginManual":
            return "manual";
            break;
        case "RadeonProRenderMayaPlugin-WeeklyFull":
            return "weekly";
            break;
        // Othres
        default:
            return "master";
            break;
    }
}