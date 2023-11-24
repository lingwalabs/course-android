package es.cursonoruego.util;

import org.junit.Test;

import es.cursonoruego.model.TaskJson;
import es.cursonoruego.util.TaskHelper;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class TaskHelperTest {

    @Test
    public void testGetValidMatchForAnswer() {
        assertThat(TaskHelper.getValidMatchForAnswer(null), nullValue());
        assertThat(TaskHelper.getValidMatchForAnswer("Ja"), is("ja"));
        assertThat(TaskHelper.getValidMatchForAnswer("Ja takk"), is("ja takk"));
        assertThat(TaskHelper.getValidMatchForAnswer("Ja takk!"), is("ja takk"));
        assertThat(TaskHelper.getValidMatchForAnswer("Ja, takk"), is("ja takk"));
        assertThat(TaskHelper.getValidMatchForAnswer("Ja takk."), is("ja takk"));
        assertThat(TaskHelper.getValidMatchForAnswer("Ja. Takk"), is("ja takk"));
        assertThat(TaskHelper.getValidMatchForAnswer("Ja. Takk."), is("ja takk"));
        assertThat(TaskHelper.getValidMatchForAnswer("Nei, jeg valgte den andre"), is("nei jeg valgte den andre"));
        assertThat(TaskHelper.getValidMatchForAnswer("Nei , jeg valgte den andre"), is("nei jeg valgte den andre"));
        assertThat(TaskHelper.getValidMatchForAnswer("Nei "), is("nei"));
        assertThat(TaskHelper.getValidMatchForAnswer("Nei  "), is("nei"));
        assertThat(TaskHelper.getValidMatchForAnswer(" Nei "), is("nei"));
    }
}
