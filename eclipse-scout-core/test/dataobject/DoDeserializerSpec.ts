/*
 * Copyright (c) 2010, 2024 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import {BaseDoEntity, dataObjects, dates, DoDeserializer, DoRegistry, ObjectFactory, typeName} from '../../src/index';

describe('DoDeserializer', () => {

  beforeAll(() => {
    ObjectFactory.get().registerNamespace('scout', {
      Fixture01Do, Fixture02Do, Fixture03Do, Fixture03SubDo
    }, {allowedReplacements: ['scout.Fixture01Do', 'scout.Fixture02Do', 'scout.Fixture03Do', 'scout.Fixture03SubDo']});

    const doRegistry = DoRegistry.get();
    doRegistry.add(Fixture01Do);
    doRegistry.add(Fixture02Do);
    doRegistry.add(Fixture03Do);
    doRegistry.add(Fixture03SubDo);
  });

  afterAll(() => {
    const doRegistry = DoRegistry.get();
    doRegistry.removeByClass(Fixture01Do);
    doRegistry.removeByClass(Fixture02Do);
    doRegistry.removeByClass(Fixture03Do);
    doRegistry.removeByClass(Fixture03SubDo);
  });

  it('can deserialize based on _type', () => {
    const json = `{
      "_type": "scout.Fixture01",
      "propBool": true,
      "propNum": 1234.5678,
      "propStr": "testString",
      "propDate": "2024-07-15 13:51:39.708Z",
      "propNull": null,
      "propArr": [
        {
          "nestedDate": "2024-07-14 13:51:39.708Z",
          "nestedObj": {
            "nestedNestedDate": "2024-07-13 13:51:39.708Z"
          },
          "nestedIfc": {
            "_type": "scout.Fixture03",
            "nestedNestedDate": "2024-07-12 13:51:39.708Z"
          }
        },
        {
          "nestedDate": "2024-07-11 13:51:39.708Z",
          "nestedObj": {
            "nestedNestedDate": "2024-07-10 13:51:39.708Z"
          },
          "nestedIfc": {
            "_type": "not.existing.but.should.survive",
            "nestedNestedDate": "2024-07-09 13:51:39.708Z"
          }
        }
      ],
      "propArr2": [[[["2024-07-02 13:51:39.708Z", "2024-07-02 12:51:39.708Z"]], [["2024-07-02 11:51:39.708Z", "2024-07-02 10:51:39.708Z"]]]],
      "propObj": {
        "nestedNestedDate": "2024-07-08 13:51:39.708Z"
      }
    }
    `;
    const dataobject = dataObjects.parse(json) as Fixture01Do; // do not pass type so that the detection is tested as well
    expect(dataobject).toBeInstanceOf(Fixture01Do);
    expect(dataobject.propBool).toBeTrue();
    expect(dataobject.propNum).toBe(1234.5678);
    expect(dataobject.propStr).toBe('testString');
    expect(dataobject.propDate).toBeInstanceOf(Date);
    expect(dataobject.propDate).toEqual(dates.parseJsonDate('2024-07-15 13:51:39.708Z'));
    expect(Array.isArray(dataobject.propArr)).toBeTrue();
    expect(dataobject.propArr.length).toBe(2);
    expect(dataobject._type).toBe('scout.Fixture01'); // does not come from deserialize but from the instance creation
    expect(dataobject.propNull).toBeNull();

    const propArr0 = dataobject.propArr[0];
    expect(propArr0).toBeInstanceOf(Fixture02Do);
    expect(propArr0.nestedDate).toEqual(dates.parseJsonDate('2024-07-14 13:51:39.708Z'));
    expect(propArr0.nestedObj).toBeInstanceOf(Fixture03Do);
    expect(propArr0.nestedObj.nestedNestedDate).toEqual(dates.parseJsonDate('2024-07-13 13:51:39.708Z'));
    expect(propArr0.nestedIfc).toBeInstanceOf(Fixture03Do);
    expect(propArr0.nestedIfc.nestedNestedDate).toEqual(dates.parseJsonDate('2024-07-12 13:51:39.708Z'));

    const propArr1 = dataobject.propArr[1];
    expect(propArr1).toBeInstanceOf(Fixture02Do);
    expect(propArr1.nestedDate).toEqual(dates.parseJsonDate('2024-07-11 13:51:39.708Z'));
    expect(propArr1.nestedObj).toBeInstanceOf(Fixture03Do);
    expect(propArr1.nestedObj.nestedNestedDate).toEqual(dates.parseJsonDate('2024-07-10 13:51:39.708Z'));
    expect(propArr1.nestedIfc).toBeInstanceOf(BaseDoEntity); // because _type is missing and the interface is not helpful
    const nested = propArr1.nestedIfc as any;
    expect(nested.nestedNestedDate).toBe('2024-07-09 13:51:39.708Z');
    expect(nested._type).toBe('not.existing.but.should.survive');

    const expectedDateArray = [[[[dates.parseJsonDate('2024-07-02 13:51:39.708Z'), dates.parseJsonDate('2024-07-02 12:51:39.708Z')]],
      [[dates.parseJsonDate('2024-07-02 11:51:39.708Z'), dates.parseJsonDate('2024-07-02 10:51:39.708Z')]]]];
    expect(dataobject.propArr2).toEqual(expectedDateArray); // is detected as array of Date with dimension 4
    expect(dataobject.propObj).toBeInstanceOf(Fixture03Do);
    expect(dataobject.propObj.nestedNestedDate).toEqual(dates.parseJsonDate('2024-07-08 13:51:39.708Z'));
  });

  it('can deserialize based on objectType', () => {
    const withStringObjectType = `{
      "objectType": "scout.Fixture03Do",
      "nestedNestedDate": "2024-07-25 09:41:10.708Z"
    }
    `;
    const resultFromStringObjectType = dataObjects.parse(withStringObjectType) as Fixture03Do;
    expect(resultFromStringObjectType).toBeInstanceOf(Fixture03Do);
    expect(resultFromStringObjectType.nestedNestedDate).toEqual(dates.parseJsonDate('2024-07-25 09:41:10.708Z'));

    const withConstructorObjectType = {
      objectType: Fixture03Do,
      nestedNestedDate: '2024-07-25 08:41:10.708Z'
    };
    const resultFromConstructorObjectType = new DoDeserializer().deserialize(withConstructorObjectType) as Fixture03Do;
    expect(resultFromConstructorObjectType).toBeInstanceOf(Fixture03Do);
    expect(resultFromConstructorObjectType.nestedNestedDate).toEqual(dates.parseJsonDate('2024-07-25 08:41:10.708Z'));
  });

  it('Uses BaseDoEntity if no type information is available', () => {
    const result = dataObjects.parse('{"num":1234}') as any;
    expect(result).toBeInstanceOf(BaseDoEntity);
    expect(result.num).toBe(1234);
  });

  it('throws if expected and given type differ', () => {
    const json = `{
      "_type": "scout.Fixture01",
      "nestedNestedDate": "2024-07-25 07:41:10.708Z"
    }
    `;
    expect(() => dataObjects.parse(json, Fixture03Do)).toThrow();
    expect(() => dataObjects.parse(json, 'scout.Fixture03Do')).toThrow();
  });

  it('can deserialize arrays', () => {
    const json = `[{
      "_type": "scout.Fixture03",
      "nestedNestedDate": "2024-07-31 07:52:39.708Z"
    }, {
      "_type": "scout.Fixture03",
      "nestedNestedDate": "2024-07-31 07:54:39.708Z"
    }]
    `;
    const arr = dataObjects.parse(json) as Fixture03Do[];
    expect(Array.isArray(arr)).toBeTrue();
    expect(arr.length).toBe(2);

    const first = arr[0];
    expect(first).toBeInstanceOf(Fixture03Do);
    expect(first._type).toBe('scout.Fixture03');
    expect(first.nestedNestedDate).toEqual(dates.parseJsonDate('2024-07-31 07:52:39.708Z'));

    const second = arr[1];
    expect(second).toBeInstanceOf(Fixture03Do);
    expect(second._type).toBe('scout.Fixture03');
    expect(second.nestedNestedDate).toEqual(dates.parseJsonDate('2024-07-31 07:54:39.708Z'));
  });

  it('can deserialize if _type is a subtype of declared type', () => {
    const json = `{
      "_type": "scout.Fixture02",
      "nestedDate": "2024-12-12 07:52:39.708Z",
      "objectType": "scout.TestObjectType",
      "nestedObj": {
        "_type": "scout.Fixture03Sub",
        "nestedNestedDateSub": "2024-12-12 08:03:39.708Z"
      }
    }
    `;
    const fixture02 = dataObjects.parse(json) as Fixture02Do;
    expect(fixture02).toBeInstanceOf(Fixture02Do);
    expect(fixture02.nestedDate).toEqual(dates.parseJsonDate('2024-12-12 07:52:39.708Z'));
    expect(fixture02['objectType']).toBe('scout.TestObjectType'); // objectType is preserved. Required e.g. for CodeTypes which may have objectType property coming from the backend
    const fixture03Sub = fixture02.nestedObj as Fixture03SubDo;
    expect(fixture03Sub).toBeInstanceOf(Fixture03SubDo); // subtype of type declared in Fixture02Do is used
    expect(fixture03Sub.nestedNestedDateSub).toEqual(dates.parseJsonDate('2024-12-12 08:03:39.708Z'));
  });
});

@typeName('scout.Fixture01')
export class Fixture01Do extends BaseDoEntity {
  propBool: boolean;
  propNum: number;
  propStr: string;
  propNull: string;
  propDate: Date;
  propArr: Fixture02Do[];
  propArr2: Array<Array<Date[]>[]>;
  propObj: Fixture03Do;
}

@typeName('scout.Fixture02')
export class Fixture02Do extends BaseDoEntity {
  nestedDate: Date;
  nestedObj: Fixture03Do;
  nestedIfc: FixtureDoIfc;
}

export interface FixtureDoIfc {
  nestedNestedDate: Date;
}

@typeName('scout.Fixture03')
export class Fixture03Do extends BaseDoEntity implements FixtureDoIfc {
  nestedNestedDate: Date;
}

@typeName('scout.Fixture03Sub')
export class Fixture03SubDo extends Fixture03Do {
  nestedNestedDateSub: Date;
}
