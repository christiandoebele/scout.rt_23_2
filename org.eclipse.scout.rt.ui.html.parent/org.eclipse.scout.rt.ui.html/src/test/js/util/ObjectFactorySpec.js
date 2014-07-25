describe("ObjectFactory", function() {

  function verifyCreationAndRegistration(session, factories) {
    session.objectFactory.register(factories);

    var i, model, factory, object, modelAdapter;
    for (i = 0; i < factories.length; i++) {
      factory = factories[i];
      model = {
        id: i,
        objectType: factory.objectType
      };

      object = null;
      try {
        object = factory.create();
        object.init(model, session);
        session.registerModelAdapter(object); //FIXME CGU remove after moving to constructor
      } catch (e) {
        //Object probably not registered, check SpecRunnerMaven.html
        expect(object).toBeTruthy();
      }

      modelAdapter = session.getModelAdapter(model);
      expect(modelAdapter).toBe(object);
    }
  }

  it("creates objects which are getting registered in the widget map", function() {
    var session = new scout.Session($('#sandbox'), '1.1');
    var factories = scout.defaultObjectFactories;

    verifyCreationAndRegistration(session, factories);
  });

  it("distinguishes between mobile and desktop objects", function() {
    var session = new scout.Session($('#sandbox'), '1.1');
    var factories = scout.mobileObjectFactories;

    verifyCreationAndRegistration(session, factories);
  });

});
