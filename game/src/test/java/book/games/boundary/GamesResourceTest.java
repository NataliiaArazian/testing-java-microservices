package book.games.boundary;

import book.games.control.GamesService;
import book.games.entity.SearchResult;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.json.Json;
import javax.json.JsonArray;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GamesResourceTest {

    // создаем моки для указанных классов
    @Mock
    GamesService gamesService;

    @Mock
    ExecutorServiceProducer executorServiceProducer;

    @Mock
    AsyncResponse asyncResponse;

    //создаем захватчик для респонса
    @Captor
    ArgumentCaptor<Response> argumentCaptorResponse;

    //создаем поток
    private static final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    //перед выполнением тестов из тестового класса открывается поток
    @Before
    public void setupExecutorServiceProducer() {
        when(executorServiceProducer.getManagedExecutorService()).thenReturn(executorService);
    }
  //после выполнения тестов из тестового класс поток закрывается
    @AfterClass
    public static void stopExecutorService() {
        executorService.shutdown();
    }

    @Test
    public void restAPIShouldSearchGamesByTheirNames() throws
            IOException, InterruptedException {
        //создаем обьект класса GamesResource, который не может быть изменен
        final GamesResource gamesResource = new GamesResource();
        //подставляем моки в gamesResource
        gamesResource.managedExecutorService = executorServiceProducer;
        gamesResource.gamesService = gamesService;

        //когда мы передаем название игры в поиске, то возвращается список результатов поиска, созданный в методе getSearchResults()
        when(gamesService.searchGames("zelda")).thenReturn
                (getSearchResults());

        //отправляем запрос на поиск игры
        gamesResource.searchGames(asyncResponse, "zelda");
        //ждать пока метод выполнится в отдельном потоке
        executorService.awaitTermination(2, TimeUnit.SECONDS);

        //проверить, что запрос выполнился, если передать ему захватчик для респонса
        verify(asyncResponse).resume(argumentCaptorResponse.capture
                ());

        //респонс - значение захваченного обьекта
        final Response response = argumentCaptorResponse.getValue()
                ; // <7>

        //проверить, что респонс имеет успешный статус код
        assertThat(response.getStatusInfo().getFamily()).isEqualTo
                (Response.Status.Family.SUCCESSFUL);

        //проверить, ято респонс содержит 2 обьекта с именем Зельда и такими-то данными
        assertThat((JsonArray) response.getEntity()).hasSize(2)
                .containsExactlyInAnyOrder(Json.createObjectBuilder
                        ().add("id", 1).add("name", "The Legend Of " +
                        "" + "Zelda").build(), Json
                        .createObjectBuilder().add("id", 2).add
                                ("name", "Zelda II: The " +
                                        "Adventure of Link").build()
        );
    }

    @Test
    public void exceptionShouldBePropagatedToCaller() throws IOException, InterruptedException {
        final GamesResource gamesResource = new GamesResource();

        gamesResource.managedExecutorService = executorServiceProducer;
        gamesResource.gamesService = gamesService;

        //когда передаем значение в поиск, то должен вернуться ексепшен ввода-вывода
        when(gamesService.searchGames("zelda")).thenThrow
                (IOException.class); // <1>
        when(gamesService.searchGames("zelda")).thenThrow(IOException.class);

        gamesResource.searchGames(asyncResponse, "zelda");
        executorService.awaitTermination(1, TimeUnit.SECONDS);

        //проверяем, что запрос выполняется и возвращается ексепшен
        verify(gamesService).searchGames("zelda");
        verify(asyncResponse).resume(any(IOException.class));
    }

    private List<SearchResult> getSearchResults() {
        final List<SearchResult> searchResults = new ArrayList<>();

        searchResults.add(new SearchResult(1, "The Legend Of Zelda"));
        searchResults.add(new SearchResult(2, "Zelda II: The Adventure of Link"));

        return searchResults;
    }
}
