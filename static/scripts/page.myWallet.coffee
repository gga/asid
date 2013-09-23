define ['jquery', 'underscore'], ($, _) ->

  insertChallengeLine = () ->
    $('form#addTrustPool .challenge').append(ich.challengeTmpl())

  insertChallengeLine()

  onAddBagItem: (handler) ->
    $('form#addBagItem').on 'submit', (e) ->
      handler($('input#newKey').val(), $('input#newValue').val())
      e.preventDefault()

  onAddChallenge: (handler) ->
    $('#addChallenge').on 'click', (e) ->
      handler()
      e.preventDefault()

  onAddTrustPool: (handler) ->
    $('form#addTrustPool').on 'submit', (e) ->
      handler($('input#poolName').val(), _.map($('.challengeEntry'), (ce) -> $(ce).val()))
      e.preventDefault()

  render: (viewMsg) ->
    if _.has(viewMsg, 'wallet')
      $('.wallet').html(ich.walletTmpl(viewMsg.wallet))
      $('.bag .entries').html(ich.bagTmpl(viewMsg.wallet))
    if _.has(viewMsg, 'addChallengeLine')
      insertChallengeLine()