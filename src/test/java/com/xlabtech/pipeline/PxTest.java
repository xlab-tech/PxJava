package com.xlabtech.pipeline;

import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class PxTest {

    @Test
    public void composePipelines() {
        Px beforePipe = new Px()
                .chain(x -> {
                    assertEquals(x, "init");
                    return "prueba";
                });

        String response = new Px<String>().chain(x -> {
            assertEquals(x, "prueba");
            return "prueba2";
        }).compose(beforePipe)
                .sync()
                .execute("init");

        assertEquals(response, "prueba2");
    }

    @Test
    public void andThenPipelines() {
        Px beforePipe = new Px()
                .chain(x -> {
                    assertEquals(x, "prueba");
                    return "prueba2";
                });

        Object response = new Px<String>().chain(x -> {
            assertEquals(x, "init");
            return "prueba";
        }).andThen(beforePipe)
                .sync()
                .execute("init");

        assertEquals(response, "prueba2");
    }

    @Test
    public void concatPipelines() {
        Px concat1 = new Px().chain(x -> {
            assertEquals(x, "prueba");
            return "prueba2";
        });

        Px concat2 = new Px().chain(x -> {
            assertEquals(x, "prueba2");
            return "prueba3";
        });

        String response = new Px<String>().chain(x -> {
            assertEquals(x, "init");
            return "prueba";
        }).concat(concat1, concat2).sync().execute("init");

        assertEquals(response, "prueba3");
    }

    @Test
    public void branchPipelines() {
        Px concat1 = new Px().chain(x -> {
            assertEquals(x, "prueba");
            return "prueba2";
        });

        Px concat2 = new Px().chain(x -> {
            assertEquals(x, "prueba");
            return "prueba3";
        });

        List response = new Px<List>().chain(x -> {
            assertEquals(x, "init");
            return "prueba";
        }).branch(concat1, concat2).sync().execute("init");

        assertEquals(response.get(0), "prueba2");
        assertEquals(response.get(1), "prueba3");
    }

    @Test
    public void verifySubscriters() {
        String response = new Px<String>().chain(x -> {
            assertEquals(x, "init");
            return "prueba";
        }, x -> {
            assertEquals(x, "prueba");
            return "prueba2";
        }).chain(x -> {
            assertEquals(x, "prueba2");
            return "prueba3";
        }).subscribeBefore((x) -> assertTrue(x instanceof String), (x) -> System.out.println(x)).sync().execute("init");

        assertEquals(response, "prueba3");
    }

    @Test
    public void callComunicationRunCorretly() {
        String response = new Px<String>()
                .<String, String>chain(x -> {
                    assertEquals(x, "hola");
                    return "adios";
                }, x -> {
                    assertEquals(x, "adios");
                    return "listo";
                }).debugEnabled().sync().execute("hola");

        assertEquals(response, "listo");
    }

    @Test
    public void testCallBatchComunucationRunSameTime() {
        List<String> response = new Px<List<String>>()
                .chainConcat(x -> {
                            System.out.println(new Date().getTime());
                            assertEquals(x, "hola");
                            return "adios";
                        }, x -> {
                            System.out.println(new Date().getTime());
                            assertEquals(x, "hola");
                            return "listo";
                        }, x -> {
                            System.out.println(new Date().getTime());
                            assertEquals(x, "hola");
                            return "listo2";
                        }
                ).sync().execute("hola");

        assertEquals(response.get(0), "adios");
        assertEquals(response.get(1), "listo");
        assertEquals(response.get(2), "listo2");
    }

    @Test
    public void combineCallAndAssyncCallPipelineUtils() {
        String response = new Px<String>()
                .chain(x -> {
                    assertEquals(x, "inicio");
                    return "hola";
                }).chain(x -> {
                    assertEquals(x, "hola");
                    return "adios";
                }).chainConcat(x -> {
                    assertEquals(x, "adios");
                    return "assync1";
                }, x -> {
                    assertEquals(x, "adios");
                    return "assync2";
                }, x -> {
                    assertEquals(x, "adios");
                    return "assync3";
                }).chain(x -> {
                    assertEquals(((List) x).get(0), "assync1");
                    assertEquals(((List) x).get(1), "assync2");
                    assertEquals(((List) x).get(2), "assync3");
                    return "final";
                }).sync().execute("inicio");

        assertEquals(response, "final");
    }

    @Test
    public void conditionalCallExecuteFunctionsIfPredicateIsFalse() {
        new Px().chainIf(x -> x.equals("hola")
                , x -> {
                    fail();
                    return "adios";
                }, x -> {
                    fail();
                    return "dios2";
                }).execute("body");
    }

    @Test
    public void conditionalCallPxIfPredicateIsFalse() {
        new Px().chainIf(x -> x.equals("hola")
                , new Px<>().chain(x -> {
                    fail();
                    return "adios";
                }, x -> {
                    fail();
                    return "dios2";
                }))
                .execute("body");
    }

    @Test
    public void conditionalCallPxIfPredicate() {
        new Px().chainIf(x -> x.equals("hola")
                , new Px<>().chain(x -> {
                    fail();
                    return "adios";
                }, x -> {
                    fail();
                    return "dios2";
                })
                , new Px().chain(x->{
                    assertEquals(x,"body");
                    return "adiosPX";
                }))
                .execute("body");
    }

    @Test
    public void conditionalCallExecuteFunctionsIfPredicateIsTrue() {
        String response = new Px<String>().<String, String>chainIf(x -> x.equals("body")
                , x -> {
                    assertEquals(x, "body");
                    return "adios";
                }, x -> {
                    assertEquals(x, "adios");
                    return "adios2";
                })
                .chainIf(x -> x.equals("adios4")
                        , x -> {
                            fail();
                            return "test";
                        }
                ).chainIf(x -> x.equals("adios2")
                        , x -> {
                            assertEquals(x, "adios2");
                            return "adios3";
                        })
                .sync()
                .execute("body");

        assertEquals(response, "adios3");
    }

    @Test(expected = RuntimeException.class)
    public void thowErrorInCall() throws InterruptedException {
        new Px().chain(x -> {
            throw new RuntimeException();
        }).sync().execute("");

        Thread.sleep(300);
    }

    @Test
    public void thowErrorInAssyncCall() throws InterruptedException {
        new Px().chain(x -> {
            throw new RuntimeException();
        }).subscribeError(err -> {
            assertEquals(err.getClass(), RuntimeException.class);
            throw (RuntimeException) err;
        }).execute("");
    }

    @Test
    public void throwErrorInCallAssync() throws InterruptedException {
        new Px().chainConcat(x -> {
            assertEquals(x, "test");
            return "hola";
        }, x -> {
            throw new RuntimeException();
        }).subscribeError(err -> {
            assertEquals(err.getClass(), RuntimeException.class);
            throw (RuntimeException) err;
        }).execute("test");

        Thread.sleep(300);
    }

    @Test
    public void executeAssyncWithConsumer() throws InterruptedException {
        new Px().chain(x -> {
            System.out.println(x.toString());
            return "test1";
        }).subscribeResult(x -> assertEquals(x, "test1"))
                .execute("init");

        Thread.sleep(300);
    }

    @Test
    public void executeAssyncWithSubscribeAfter() {
        new Px<>().subscribeAfter(x -> {
            System.out.println(x);
            assertEquals(x.getClass(), String.class);
        }).chain(x -> "hola", x -> "hola1", x -> "hola2").execute("init");
    }

}