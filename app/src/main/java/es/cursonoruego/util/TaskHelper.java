package es.cursonoruego.util;

public class TaskHelper {

    /**
     * Examples:
     *
     *    "Ja takk!" --> "ja takk"
     *    "Ja, takk" --> "ja takk"
     *    "Ja. Takk." --> "ja takk"
     *
     * @param correctAnswer One of the Tasks correct answers
     * @return The text representation of the Task, minus signs
     */
    public static String getValidMatchForAnswer(String correctAnswer) {
        if (correctAnswer == null) {
            return null;
        }
        String taskTextWithoutSigns = correctAnswer;

        taskTextWithoutSigns = taskTextWithoutSigns.replace("!", "");
        taskTextWithoutSigns = taskTextWithoutSigns.replace("?", "");
        taskTextWithoutSigns = taskTextWithoutSigns.replace(",", "");
        taskTextWithoutSigns = taskTextWithoutSigns.replace(".", "");
        taskTextWithoutSigns = taskTextWithoutSigns.replace("  ", " ");
        taskTextWithoutSigns = taskTextWithoutSigns.toLowerCase();
        taskTextWithoutSigns = taskTextWithoutSigns.trim();

        return taskTextWithoutSigns;
    }
}
