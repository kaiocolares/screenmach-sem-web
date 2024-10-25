package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "http://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=ac99ce0c";
    private List<DadosSerie> dadosSeries = new ArrayList<>();

    private SerieRepository repository;

    private List<Serie> series = new ArrayList<>();

    private Optional<Serie> serieBuscada;

    public Principal(SerieRepository repository) {
        this.repository = repository;
    }

    public void exibeMenu() {

        var opcao = -1;
        while (opcao != 0) {
            var menu = """
                    1  - Buscar séries
                    2  - Buscar episódios
                    3  - Listar séries buscadas
                    4  - Buscar série por título
                    5  - Buscar séries por ator
                    6  - Top 5 séries
                    7  - Buscar séries por gênero
                    8  - Filtar séries
                    9  - Buscar episódio por trecho
                    10 - Top Episódios por Série
                    11 - Buscar episódios a partir de uma data
                    
                    0 - Sair
                    """;
            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriesPorAtor();
                    break;
                case 6:
                    buscarTopSeries();
                    break;
                case 7:
                    buscarSeriesPorGenero();
                    break;
                case 8:
                    buscarSeriesFiltradas();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosData();
                    break;
                case 0:
                    System.out.println("Saindo ...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }


    }


    private void buscarSerieWeb() {
            DadosSerie dados = getDadosSerie();
           // dadosSeries.add(dados);
            Serie serie = new Serie(dados);
            repository.save(serie);
            System.out.println(dados);
        }

        private DadosSerie getDadosSerie() {
            System.out.println("Digite o nome da série para busca: ");
            var nomeSerie = leitura.nextLine();
            var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
            DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
            return dados;
        }

        private void buscarEpisodioPorSerie() {
            listarSeriesBuscadas();
            System.out.println("Escolha uma serie pelo nome: ");
            var nomeSerie = leitura.nextLine();

            Optional<Serie> serie = repository.findByTituloContainingIgnoreCase(nomeSerie);

            if (serie.isPresent()) {
                var serieEncontrada = serie.get();
                List<DadosTemporada> temporadas = new ArrayList<>();

                for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                    var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                    DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                    temporadas.add(dadosTemporada);
                }
                temporadas.forEach(System.out::println);
                List<Episodio> episodios = temporadas.stream()
                        .flatMap(d -> d.episodios().stream()
                                .map(e -> new Episodio(d.numero(), e)))
                        .collect(Collectors.toList());
                serieEncontrada.setEpisodios(episodios);
                repository.save(serieEncontrada);
            } else {
                System.out.println("Serie não encontrada!");
            }
        }

        private void listarSeriesBuscadas() {
        series = repository.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
        }

    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();
        serieBuscada = repository.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBuscada.isPresent()) {
            System.out.println("Dados da série buscada: " + serieBuscada.get());
        } else {
            System.out.println("Série não encontrada");
        }
    }

    private void buscarSeriesPorAtor() {
        System.out.println("Digite o nome do ator: ");
        var nomeAtor = leitura.nextLine();

        System.out.println("Digite a avaliação mínima desejada da série: ");
        var avaliacao = leitura.nextDouble();

        List<Serie> seriesEncontradas = repository.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);
        System.out.println("Series em que o " + nomeAtor + " trabalhou:");
        seriesEncontradas.forEach(se -> System.out.println(se.getTitulo() + ", avaliada em: " + se.getAvaliacao()));
    }

    private void buscarTopSeries() {
        List<Serie> topSeries = repository.findTop5ByOrderByAvaliacaoDesc();
        topSeries.forEach(se -> System.out.println(se.getTitulo() + ", avaliada em: " + se.getAvaliacao()));
    }

    private void buscarSeriesPorGenero() {
        System.out.println("Deseja série de que gênero ?");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);

        List<Serie> seriesPorCategoria = repository.findByGenero(categoria);
        System.out.println("Series do gênero: " + nomeGenero);
        seriesPorCategoria.forEach(sc -> System.out.println(sc.getTitulo() + ", avaliada em: " + sc.getAvaliacao()));
    }

    private void buscarSeriesFiltradas() {
        System.out.println("Digite o número máximo de temporadas: ");
        var maxTemporadas = leitura.nextInt();
        leitura.nextLine();
        System.out.println("Digite a avaliação mínima desejada: ");
        var avaliacaoMinima = leitura.nextDouble();

        List<Serie> seriesFiltradas = repository.findSerieFiltrada(maxTemporadas, avaliacaoMinima);
        System.out.println("Séries encontradas: ");
        seriesFiltradas.forEach(sc -> System.out.println(sc.getTitulo() + ", avaliada em: " + sc.getAvaliacao()
                + ", contendo " + sc.getTotalTemporadas() + " temporadas"));
    }

    private void buscarEpisodioPorTrecho() {
        System.out.println("Qual o nome do episódio para busca ?");
        var trechoEpisodio = leitura.nextLine();

        List<Episodio> episodiosEncontrados = repository.episodiosPorTrecho(trechoEpisodio);
        System.out.println("Lista de episodios encontrados: ");
        episodiosEncontrados.forEach(sc -> System.out.printf("Série: %s | Temporada: %s - Episódio: %s | Avaliação: %s \n",
                sc.getSerie().getTitulo(), sc.getTemporada(), sc.getNumeroEpisodio(), sc.getAvaliacao()));
    }

    private void topEpisodiosPorSerie() {
        buscarSeriePorTitulo();
        if (serieBuscada.isPresent()) {
            Serie serie = serieBuscada.get();
            List<Episodio> topEpisodios = repository.topEpisodiosSerie(serie);
            System.out.printf("Lista dos melhores episódios da série %s \n", serie.getTitulo());
            topEpisodios.forEach(sc -> System.out.printf("Episódio: %s | Temporada: %s - Episódio: %s | Avaliação: %s \n",
                    sc.getTitulo(), sc.getTemporada(), sc.getNumeroEpisodio(), sc.getAvaliacao()));
        }
    }

    private void buscarEpisodiosData() {
        buscarSeriePorTitulo();
        if (serieBuscada.isPresent()) {
            Serie serie = serieBuscada.get();
            System.out.println("Digite a partir de qual ano você quer os episódios");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();

            List<Episodio> episodiosAno = repository.episodioPorSerieEAno(serie, anoLancamento);
            System.out.printf("Lista dos episódios de %s a partir do ano %s \n", serie.getTitulo(), anoLancamento);
            episodiosAno.forEach(sc -> System.out.printf("Episódio: %s | Temporada: %s - Episódio: %s | Avaliação: %s \n",
                    sc.getTitulo(), sc.getTemporada(), sc.getNumeroEpisodio(), sc.getAvaliacao()));
        }
    }
}

//        PROGRAMA ANTIGO
//        System.out.println("Digite o nome da série para busca:");
//        var nomeSerie = leitura.nextLine();
//        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
//        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
//        System.out.println(dados);
//
//        List<DadosTemporada> temporadas = new ArrayList<>();
//
//        for (int i = 1; i <= dados.totalTemporadas(); i++) {
//            json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + "&season=" + i + API_KEY);
//            DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
//            temporadas.add(dadosTemporada);
//        }
//        temporadas.forEach(System.out::println);
//
//        temporadas.forEach(t -> t.episodios().forEach(e -> System.out.println(e.titulo())));
//
//        List<DadosEpisodio> dadosEpisodios = temporadas.stream()
//                .flatMap(t -> t.episodios().stream())
//                .collect(Collectors.toList());
//        dadosEpisodios.stream()
//                .filter(e -> !e.avaliacao().equalsIgnoreCase("N/A"))
//                .sorted(Comparator.comparing(DadosEpisodio::avaliacao).reversed())
//                .limit(5)
//                .forEach(System.out::println);
//
//        List<Episodio> episodios = temporadas.stream()
//                .flatMap(t -> t.episodios().stream()
//                        .map(d -> new Episodio(t.numero(), d))
//                ).collect(Collectors.toList());
//        episodios.forEach(System.out::println);

//        System.out.println("Digite um trecho do título do episódio para buscá-lo");
//        var trechoEpisodio = leitura.nextLine();
//
//        Optional<Episodio> localizaEpisodio = episodios.stream()
//                .filter(e -> e.getTitulo().toLowerCase().contains(trechoEpisodio.toLowerCase()))
//                .findFirst();
//
//        if (localizaEpisodio.isPresent()) {
//            System.out.println("O episódio " + localizaEpisodio.get().getTitulo() +
//                    "esta na temporada " + localizaEpisodio.get().getTemporada());
//        }
//        else {
//            System.out.println("Episódio não encontrado");
//        }

//        System.out.println("Você deseja ver os episódios a partir de que ano ?");
//        var ano = leitura.nextInt();
//        leitura.nextLine();
//
//        LocalDate dataBusca = LocalDate.of(ano, 1, 1);
//
//        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy");
//
//        episodios.stream()
//                .filter(e -> e.getDataLancamento() != null && e.getDataLancamento().isAfter(dataBusca))
//                .forEach(e -> System.out.println(
//                        "Temporada: " + e.getTemporada() +
//                                " Episodio: " + e.getTitulo() +
//                                " Data lançamento: " + e.getDataLancamento().format(formatador)
//                ));

//        Map<Integer, Double> avaliacaoPorTemporada = episodios.stream()
//                .filter(e -> e.getAvaliacao() > 0.0)
//                .collect(Collectors.groupingBy(Episodio::getTemporada,
//                        Collectors.averagingDouble(Episodio::getAvaliacao)));
//        System.out.println(avaliacaoPorTemporada);
//
//        DoubleSummaryStatistics est = episodios.stream()
//                .filter(e -> e.getAvaliacao() > 0.0)
//                .collect(Collectors.summarizingDouble(Episodio::getAvaliacao));
//        System.out.println("Media da avaliação dos episódios: " + est.getAverage());
//        System.out.println("Número de episódios avaliados: " + est.getCount());
//        System.out.println("Melhor avaliação: " + est.getMax());
//        System.out.println("Pior avaliação: " + est.getMin());
