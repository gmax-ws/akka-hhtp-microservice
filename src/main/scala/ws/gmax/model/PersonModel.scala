package ws.gmax.model

import io.swagger.annotations._

import scala.annotation.meta.field

@ApiModel(value = "Person")
case class Person(
                   @(ApiModelProperty@field)(
                     value = "person unique id",
                     name = "id",
                     dataType = "int",
                     example = "1")
                   id: Int,
                   @(ApiModelProperty@field)(
                     value = "person name",
                     name = "name",
                     dataType = "string",
                     example = "John D. Morris")
                   name: String,
                   @(ApiModelProperty@field)(
                     value = "person address",
                     name = "address",
                     dataType = "string",
                     example = "Downing Street 10, London, UK")
                   address: String,
                   @(ApiModelProperty@field)(
                     value = "person age",
                     name = "age",
                     dataType = "int",
                     example = "25")
                   age: Int)

@ApiModel(value = "Persons")
case class Persons(
                    @(ApiModelProperty@field)(
                      value = "list of persons",
                      name = "persons",
                      dataType = "List[ws.gmax.model.Person]")
                    persons: List[Person])
